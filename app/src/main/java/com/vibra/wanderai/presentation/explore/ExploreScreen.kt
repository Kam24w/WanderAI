package com.vibra.wanderai.presentation.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WrongLocation
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.BuildConfig
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.TravelMode
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.presentation.common.formatCopShort
import com.vibra.wanderai.presentation.common.formatDistance
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.presentation.common.labelRes
import com.vibra.wanderai.presentation.common.localized
import com.vibra.wanderai.presentation.common.messageRes
import com.vibra.wanderai.presentation.common.travelTimeLabel
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.AppPermissions
import com.vibra.wanderai.presentation.components.IconBadge
import com.vibra.wanderai.presentation.components.StatusPill
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.presentation.components.rememberPermissionState
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ExploreScreen(
    onOpenVibra: () -> Unit,
    onGenerateItinerary: () -> Unit,
) {
    val viewModel = wanderViewModel {
        ExploreViewModel(
            it.placesRepository, it.itineraryRepository, it.vibraRepository,
            it.syncItineraryToWatch, it.wearDataLayerRepository, it.observeTravelerLocation, it.planSession,
            it.routesRepository, it.startNavigationToPlace, it.settingsRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val viewport = rememberMapViewport()
    val googleMap = rememberGoogleMapController()
    // Google Maps SDK when MAPS_API_KEY is configured; offline canvas map otherwise.
    val useGoogleMap = BuildConfig.HAS_MAPS_API_KEY
    val mapController: ExploreMapController = if (useGoogleMap) googleMap else remember(viewport) { CanvasMapController(viewport) }
    val scope = rememberCoroutineScope()

    val locationPermission = rememberPermissionState(AppPermissions.location, requireAll = false)
    var askedForLocation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!locationPermission.isGranted && !askedForLocation) {
            askedForLocation = true
            locationPermission.request()
        }
    }
    LaunchedEffect(locationPermission.isGranted) {
        if (locationPermission.isGranted) viewModel.refreshLocation()
    }

    // Recommendation tapped in the chat / itinerary: select it and fly there smoothly.
    LaunchedEffect(state.focusRequest) {
        val request = state.focusRequest ?: return@LaunchedEffect
        viewModel.onFocusHandled(request)
        mapController.flyTo(request.place.location)
    }

    // Real streets from the Routes API (straight segments while loading or if it fails).
    val route = state.routePath.points

    // After "Fijar rumbo": frame the whole path once the street route is drawn.
    LaunchedEffect(state.pendingRouteFit, state.routePath) {
        val course = state.course as? CourseState.Started ?: return@LaunchedEffect
        if (!state.pendingRouteFit || route.size < 2) return@LaunchedEffect
        if (course.followsStreets && !state.routePath.followsStreets) return@LaunchedEffect
        viewModel.onRouteFitted()
        mapController.showPath(route)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        WanderTopBar(
            title = stringResource(R.string.explore_title),
            actions = {
                if (state.activeItinerary != null) {
                    IconButton(onClick = viewModel::clearRoute) {
                        Icon(Icons.Outlined.WrongLocation, contentDescription = stringResource(R.string.explore_clear_route))
                    }
                }
                IconButton(onClick = onOpenVibra) {
                    Icon(
                        Icons.Outlined.Watch,
                        contentDescription = stringResource(R.string.tab_vibra),
                        tint = if (state.connection is VibraConnectionState.Connected) WanderTheme.colors.success
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )

        Box(Modifier.fillMaxSize()) {
            val highlighted = state.routePlaceIds
            if (useGoogleMap) {
                WanderGoogleMap(
                    controller = googleMap,
                    places = state.visiblePlaces,
                    highlightedPlaceIds = highlighted,
                    route = route,
                    userLocation = state.location.point,
                    selectedPlaceId = state.selectedPlaceId,
                    myLocationEnabled = locationPermission.isGranted,
                    onPlaceClick = { place ->
                        viewModel.selectPlace(place.id)
                        scope.launch { mapController.flyTo(place.location) }
                    },
                    onMapClick = { viewModel.selectPlace(null) },
                    mapDescription = stringResource(R.string.explore_map_description),
                    // Keeps the focused marker above the floating cards.
                    contentPadding = PaddingValues(bottom = 220.dp),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PastoMap(
                    viewport = viewport,
                    places = state.visiblePlaces,
                    highlightedPlaceIds = highlighted,
                    route = route,
                    userLocation = state.location.point,
                    selectedPlaceId = state.selectedPlaceId,
                    onPlaceClick = { viewModel.selectPlace(it.id) },
                    onMapClick = { viewModel.selectPlace(null) },
                    mapDescription = stringResource(R.string.explore_map_description),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Floating controls (right).
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MapFab(Icons.Outlined.MyLocation, stringResource(R.string.explore_my_location)) {
                    if (!locationPermission.isGranted) locationPermission.request()
                    scope.launch { mapController.flyTo(state.location.point) }
                }
                MapFab(Icons.Outlined.Add, stringResource(R.string.explore_zoom_in)) { scope.launch { mapController.zoomIn() } }
                MapFab(Icons.Outlined.Remove, stringResource(R.string.explore_zoom_out)) { scope.launch { mapController.zoomOut() } }
            }

            // GPS + recommended filter (top-left).
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    text = stringResource(if (state.location.isLive) R.string.gps_live else R.string.gps_reference),
                    color = if (state.location.isLive) WanderTheme.colors.success else WanderTheme.colors.brandGold,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    container = MaterialTheme.colorScheme.surface,
                )
                FilterChip(
                    selected = state.showRecommended,
                    onClick = viewModel::toggleRecommended,
                    label = { Text(stringResource(R.string.explore_ai_pins), fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = WanderTheme.colors.brandTeal,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        iconColor = WanderTheme.colors.brandGold,
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedVisibility(
                    visible = state.selectedPlace != null,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                ) {
                    state.selectedPlace?.let { place ->
                        PlaceDetailCard(
                            place = place,
                            distanceMeters = state.location.point.distanceTo(place.location).roundToInt(),
                            course = state.course.takeIf { it.placeId() == place.id } ?: CourseState.Idle,
                            isActiveCourse = state.isCurrentCourseTo(place.id),
                            travelMode = state.travelMode,
                            onSelectTravelMode = viewModel::selectTravelMode,
                            onSetCourse = { viewModel.setCourse(place) },
                            onClose = { viewModel.selectPlace(null) },
                        )
                    }
                }
                WatchSyncCard(
                    state = state,
                    onSync = viewModel::syncToWatch,
                    onGenerate = onGenerateItinerary,
                )
            }
        }
    }
}

@Composable
private fun MapFab(icon: ImageVector, description: String, onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
    ) {
        Icon(icon, contentDescription = description)
    }
}

private fun CourseState.placeId(): String? = when (this) {
    CourseState.Idle -> null
    is CourseState.Starting -> placeId
    is CourseState.Started -> placeId
}

@Composable
private fun PlaceDetailCard(
    place: Place,
    distanceMeters: Int,
    course: CourseState,
    isActiveCourse: Boolean,
    travelMode: TravelMode,
    onSelectTravelMode: (TravelMode) -> Unit,
    onSetCourse: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            IconBadge(icon = place.category.icon, tint = place.category.color(), size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(place.category.labelRes) + " · " + formatDistance(distanceMeters) + " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(Icons.Filled.Star, null, tint = WanderTheme.colors.brandGold, modifier = Modifier.size(14.dp))
                    Text(place.rating.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = " · " + if (place.averageCostCop == 0) stringResource(R.string.common_free) else formatCopShort(place.averageCostCop),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(place.summary.localized(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
                TravelModeSelector(
                    selected = travelMode,
                    enabled = course !is CourseState.Starting,
                    onSelect = onSelectTravelMode,
                )
                Spacer(Modifier.height(8.dp))
                CourseAction(course = course, isActiveCourse = isActiveCourse, onSetCourse = onSetCourse)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** "A pie · Moto · Carro": how the route of the next "Fijar rumbo" is calculated. */
@Composable
private fun TravelModeSelector(selected: TravelMode, enabled: Boolean, onSelect: (TravelMode) -> Unit) {
    val modes = TravelMode.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            val label = stringResource(mode.labelRes)
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = WanderTheme.colors.brandTeal.copy(alpha = 0.16f),
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    activeBorderColor = WanderTheme.colors.brandTeal,
                ),
                icon = { Icon(mode.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
            )
        }
    }
}

/** "Fijar rumbo" button, or what happened after pressing it. */
@Composable
private fun CourseAction(course: CourseState, isActiveCourse: Boolean, onSetCourse: () -> Unit) {
    val started = course as? CourseState.Started
    if (isActiveCourse && started != null) {
        val message = when {
            !started.followsStreets -> R.string.explore_course_straight
            started.watchSynced -> R.string.explore_course_synced
            else -> R.string.explore_course_no_watch
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(
                Icons.Outlined.Navigation,
                contentDescription = null,
                tint = if (started.watchSynced) WanderTheme.colors.success else WanderTheme.colors.brandGold,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(message), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        return
    }
    val starting = course is CourseState.Starting
    Button(
        onClick = onSetCourse,
        enabled = !starting,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WanderTheme.colors.brandTeal, contentColor = Color.White),
        modifier = Modifier.height(40.dp),
    ) {
        if (starting) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.explore_course_starting), fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (isActiveCourse) R.string.explore_course_resend else R.string.explore_set_course), fontWeight = FontWeight.Bold)
        }
    }
}

/** Navy floating card from the reference: "WATCH SYNC STATUS · WanderAI → Vibra". */
@Composable
private fun WatchSyncCard(
    state: ExploreUiState,
    onSync: () -> Unit,
    onGenerate: () -> Unit,
) {
    val colors = WanderTheme.colors
    // Either the BLE/simulated Vibra or the Wear OS app can receive the route.
    val connected = state.connection is VibraConnectionState.Connected || state.wearConnected
    val itinerary = state.activeItinerary
    val sync = state.syncStatus

    val (pillText, pillColor) = when {
        state.isActiveItinerarySynced -> stringResource(R.string.sync_pill_synced) to colors.success
        sync is WatchSyncStatus.Sending -> stringResource(R.string.sync_pill_sending) to colors.brandGold
        connected -> stringResource(R.string.sync_pill_connected) to colors.brandTeal
        else -> stringResource(R.string.sync_pill_offline) to Color.White.copy(alpha = 0.5f)
    }

    val message = when {
        itinerary == null -> stringResource(R.string.sync_msg_no_itinerary)
        sync is WatchSyncStatus.Sending -> stringResource(R.string.sync_msg_sending)
        state.isActiveItinerarySynced && sync is WatchSyncStatus.Synced ->
            stringResource(R.string.sync_msg_synced, travelTimeLabel(sync.firstStopTravelMinutes, sync.travelMode), sync.firstStopName)
        sync is WatchSyncStatus.Failed -> stringResource(sync.error.messageRes)
        else -> pluralStringResource(R.plurals.sync_msg_ready, itinerary.stops.size, itinerary.stops.size)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.syncCard,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.accentWarm),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Watch, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sync_title),
                        color = colors.accentWarm,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = stringResource(R.string.sync_route_label),
                        color = colors.onSyncCard,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusPill(text = pillText, color = pillColor, contentColor = colors.onSyncCard, container = Color.White.copy(alpha = 0.1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(message, color = colors.onSyncCardMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            val (label, action, icon) = when {
                itinerary == null -> Triple(stringResource(R.string.sync_action_generate), onGenerate, Icons.Outlined.AutoAwesome)
                // No BLE link required: the route always goes out over the Wear OS Data Layer.
                state.isActiveItinerarySynced -> Triple(stringResource(R.string.sync_action_resend), onSync, Icons.Outlined.Sync)
                else -> Triple(stringResource(R.string.sync_action_sync), onSync, Icons.Outlined.Sync)
            }
            Button(
                onClick = action,
                enabled = sync !is WatchSyncStatus.Sending,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandTeal, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                if (sync is WatchSyncStatus.Sending) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
