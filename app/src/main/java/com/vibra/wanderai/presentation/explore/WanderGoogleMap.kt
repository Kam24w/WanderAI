package com.vibra.wanderai.presentation.explore

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.ui.theme.WanderTheme

/** Camera moves used by the Explore screen, whatever map is showing. */
interface ExploreMapController {
    /** Smoothly flies to [point] (used when a recommendation is tapped). */
    suspend fun flyTo(point: GeoPoint)
    suspend fun zoomIn()
    suspend fun zoomOut()
}

/** Controller for the Google Maps SDK view. */
class GoogleMapController(val cameraPositionState: CameraPositionState) : ExploreMapController {
    override suspend fun flyTo(point: GeoPoint) {
        val zoom = cameraPositionState.position.zoom.coerceAtLeast(FOCUS_ZOOM)
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoom), FLY_DURATION_MS)
    }

    override suspend fun zoomIn() = cameraPositionState.animate(CameraUpdateFactory.zoomIn(), ZOOM_DURATION_MS)
    override suspend fun zoomOut() = cameraPositionState.animate(CameraUpdateFactory.zoomOut(), ZOOM_DURATION_MS)

    private companion object {
        const val FOCUS_ZOOM = 17f
        const val FLY_DURATION_MS = 900
        const val ZOOM_DURATION_MS = 300
    }
}

/** Controller for the offline canvas map ([PastoMap]) used when there is no MAPS_API_KEY. */
class CanvasMapController(private val viewport: MapViewport) : ExploreMapController {
    override suspend fun flyTo(point: GeoPoint) = viewport.animateTo(point, targetScale = 1.6f)
    override suspend fun zoomIn() = viewport.zoomBy(1.4f)
    override suspend fun zoomOut() = viewport.zoomBy(1 / 1.4f)
}

@Composable
fun rememberGoogleMapController(): GoogleMapController {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(GeoPoint.PASTO_CENTER.toLatLng(), INITIAL_ZOOM)
    }
    return remember(cameraPositionState) { GoogleMapController(cameraPositionState) }
}

/**
 * Explore map on the Google Maps SDK: catalogue markers tinted by category,
 * the active route as a polyline and the traveler's position.
 */
@Composable
fun WanderGoogleMap(
    controller: GoogleMapController,
    places: List<Place>,
    highlightedPlaceIds: Set<String>,
    route: List<GeoPoint>,
    userLocation: GeoPoint,
    selectedPlaceId: String?,
    myLocationEnabled: Boolean,
    onPlaceClick: (Place) -> Unit,
    onMapClick: () -> Unit,
    mapDescription: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val hues = PlaceCategory.entries.associateWith { it.color().hue() }
    val routeColor = WanderTheme.colors.accentWarm
    val userColor = WanderTheme.colors.brandTeal

    GoogleMap(
        modifier = modifier,
        cameraPositionState = controller.cameraPositionState,
        contentDescription = mapDescription,
        contentPadding = contentPadding,
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
        ),
        onMapClick = { onMapClick() },
    ) {
        if (route.size >= 2) {
            Polyline(points = route.map(GeoPoint::toLatLng), color = routeColor, width = 14f, geodesic = true)
        }
        places.forEach { place ->
            key(place.id) {
                val selected = place.id == selectedPlaceId
                val dimmed = highlightedPlaceIds.isNotEmpty() && place.id !in highlightedPlaceIds
                Marker(
                    state = rememberUpdatedMarkerState(position = place.location.toLatLng()),
                    title = place.name,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (selected) BitmapDescriptorFactory.HUE_AZURE else hues.getValue(place.category),
                    ),
                    alpha = if (dimmed && !selected) 0.5f else 1f,
                    zIndex = if (selected) 2f else if (dimmed) 0f else 1f,
                    onClick = {
                        onPlaceClick(place)
                        true // the screen animates the camera and shows its own detail card
                    },
                )
            }
        }
        // Without the location permission the SDK blue dot is not available.
        if (!myLocationEnabled) {
            Circle(
                center = userLocation.toLatLng(),
                radius = 14.0,
                fillColor = userColor,
                strokeColor = Color.White,
                strokeWidth = 6f,
                zIndex = 3f,
            )
        }
    }
}

fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private const val INITIAL_ZOOM = 15f

private fun Color.hue(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[0]
}
