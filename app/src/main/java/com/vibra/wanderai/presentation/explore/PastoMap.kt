package com.vibra.wanderai.presentation.explore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.ui.theme.WanderExtendedColors
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Pan / zoom state of [PastoMap]. World coordinates are metres east/south of
 * [GeoPoint.PASTO_CENTER]; [offset] is the screen translation of that origin
 * relative to the viewport centre.
 */
@Stable
class MapViewport {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    internal var size by mutableStateOf(IntSize.Zero)

    /** Pixels per metre at scale 1: ~2.6 km visible across. */
    internal val basePixelsPerMeter: Float get() = if (size.width == 0) 0f else size.width / VISIBLE_METERS

    internal fun project(point: GeoPoint): Offset {
        val world = toWorld(point)
        val ppm = basePixelsPerMeter * scale
        return Offset(size.width / 2f + offset.x + world.x * ppm, size.height / 2f + offset.y + world.y * ppm)
    }

    internal fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val factor = newScale / scale
        val viewCenter = Offset(size.width / 2f, size.height / 2f)
        offset = (centroid - viewCenter) - (centroid - viewCenter - offset) * factor + pan
        scale = newScale
    }

    fun zoomBy(factor: Float) = transform(Offset(size.width / 2f, size.height / 2f), Offset.Zero, factor)

    suspend fun animateTo(point: GeoPoint, targetScale: Float = scale) {
        val world = toWorld(point)
        val start = offset
        val startScale = scale
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(450)) {
            val s = startScale + (targetScale - startScale) * value
            val ppm = basePixelsPerMeter * s
            val target = Offset(-world.x * ppm, -world.y * ppm)
            scale = s
            offset = start + (target - start) * value
        }
    }

    companion object {
        private const val VISIBLE_METERS = 2_600f
        private const val MIN_SCALE = 0.6f
        private const val MAX_SCALE = 4f
        private const val METERS_PER_DEG_LAT = 110_540.0
        private val METERS_PER_DEG_LON = 111_320.0 * cos(Math.toRadians(GeoPoint.PASTO_CENTER.latitude))

        internal fun toWorld(point: GeoPoint): Offset = Offset(
            ((point.longitude - GeoPoint.PASTO_CENTER.longitude) * METERS_PER_DEG_LON).toFloat(),
            (-(point.latitude - GeoPoint.PASTO_CENTER.latitude) * METERS_PER_DEG_LAT).toFloat(),
        )
    }
}

@Composable
fun rememberMapViewport(): MapViewport = remember { MapViewport() }

/** Stylised vector map of Pasto's centre with custom pins, route and user dot. */
@Composable
fun PastoMap(
    viewport: MapViewport,
    places: List<Place>,
    highlightedPlaceIds: Set<String>,
    route: List<GeoPoint>,
    userLocation: GeoPoint,
    selectedPlaceId: String?,
    onPlaceClick: (Place) -> Unit,
    onMapClick: () -> Unit,
    mapDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = WanderTheme.colors
    val pulse = rememberInfiniteTransition(label = "userPulse")
    val pulseProgress by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { viewport.size = it }
            .pointerInput(viewport) {
                detectTransformGestures { centroid, pan, zoom, _ -> viewport.transform(centroid, pan, zoom) }
            }
            .pointerInput(viewport) { detectTapGestures(onTap = { onMapClick() }) }
            .semantics { contentDescription = mapDescription },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (viewport.size == IntSize.Zero) return@Canvas
            drawBaseMap(viewport, colors)
            drawRoute(viewport, route, colors)
            drawUser(viewport.project(userLocation), pulseProgress, colors)
        }

        places.forEach { place ->
            val highlighted = place.id in highlightedPlaceIds
            val selected = place.id == selectedPlaceId
            PlacePin(
                place = place,
                showLabel = highlighted || selected,
                emphasized = selected,
                dimmed = highlightedPlaceIds.isNotEmpty() && !highlighted && !selected,
                onClick = { onPlaceClick(place) },
                modifier = Modifier.anchorBottomCenter { viewport.project(place.location) },
            )
        }
    }
}

/** Places the element so its bottom-centre sits on [position]. */
private fun Modifier.anchorBottomCenter(position: () -> Offset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val p = position()
        placeable.place((p.x - placeable.width / 2f).roundToInt(), (p.y - placeable.height).roundToInt())
    }
}

@Composable
private fun PlacePin(
    place: Place,
    showLabel: Boolean,
    emphasized: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = place.category.color()
    val headSize = if (emphasized) 40.dp else 32.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showLabel) {
            Row(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .widthIn(max = 150.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = place.rating.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WanderTheme.colors.brandGold,
                )
                Icon(Icons.Filled.Star, null, tint = WanderTheme.colors.brandGold, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.size(3.dp))
        }
        Box(
            modifier = Modifier
                .size(headSize)
                .shadow(if (dimmed) 0.dp else 5.dp, CircleShape)
                .background(if (dimmed) tint.copy(alpha = 0.45f) else tint, CircleShape)
                .border(2.5.dp, Color.White, CircleShape)
                .clickable(onClickLabel = place.name, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(place.category.icon, contentDescription = place.name, tint = Color.White, modifier = Modifier.size(headSize * 0.52f))
        }
        Canvas(Modifier.size(width = 12.dp, height = 8.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, if (dimmed) tint.copy(alpha = 0.45f) else tint)
        }
    }
}

// ── Drawing ───────────────────────────────────────────────────────────────────

/** Approximate course of the Río Pasto through the centre (world metres). */
private val RIVER = listOf(
    Offset(-1900f, 1700f), Offset(-1150f, 1050f), Offset(-450f, 720f), Offset(150f, 380f),
    Offset(650f, -60f), Offset(1050f, -620f), Offset(1500f, -1150f), Offset(2100f, -1700f),
)

private const val STREET_SPACING_M = 105f
private const val GRID_EXTENT_M = 2_600f
private const val GRID_ROTATION_DEG = -9f
private const val PARK_RADIUS_M = 85f
private const val RIVER_WIDTH_M = 28f

private fun DrawScope.drawBaseMap(viewport: MapViewport, colors: WanderExtendedColors) {
    drawRect(colors.mapBlock)
    val ppm = viewport.basePixelsPerMeter * viewport.scale
    val origin = Offset(size.width / 2f + viewport.offset.x, size.height / 2f + viewport.offset.y)

    // Street grid, rotated like Pasto's colonial layout.
    rotate(GRID_ROTATION_DEG, pivot = origin) {
        val minorWidth = (7f * viewport.scale.coerceAtMost(2f)).coerceAtLeast(3f)
        val majorWidth = minorWidth * 1.9f
        var i = -GRID_EXTENT_M
        var index = 0
        while (i <= GRID_EXTENT_M) {
            val major = index % 5 == 0
            val color = if (major) colors.mapRoadMajor else colors.mapRoad
            val width = if (major) majorWidth else minorWidth
            val p = i * ppm
            drawLine(color, Offset(origin.x + p, origin.y - GRID_EXTENT_M * ppm), Offset(origin.x + p, origin.y + GRID_EXTENT_M * ppm), width)
            drawLine(color, Offset(origin.x - GRID_EXTENT_M * ppm, origin.y + p), Offset(origin.x + GRID_EXTENT_M * ppm, origin.y + p), width)
            i += STREET_SPACING_M
            index++
        }
    }

    // Green areas around parks and viewpoints.
    PARK_CENTERS.forEach { center ->
        val c = viewport.project(center)
        drawRoundRect(
            color = colors.mapPark,
            topLeft = Offset(c.x - PARK_RADIUS_M * ppm, c.y - PARK_RADIUS_M * ppm),
            size = Size(PARK_RADIUS_M * 2 * ppm, PARK_RADIUS_M * 2 * ppm),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f * viewport.scale),
        )
    }

    // River.
    val river = Path().apply {
        RIVER.forEachIndexed { index, world ->
            val p = Offset(origin.x + world.x * ppm, origin.y + world.y * ppm)
            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
    }
    drawPath(river, colors.mapWater, style = Stroke(width = (RIVER_WIDTH_M * ppm).coerceAtLeast(6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private val PARK_CENTERS = listOf(
    GeoPoint(1.2137, -77.2811), // Plaza de Nariño
    GeoPoint(1.2205, -77.2752), // Parque Infantil
    GeoPoint(1.2062, -77.2731), // Parque Bolívar
    GeoPoint(1.2208, -77.2884), // Loma del Centenario
)

private fun DrawScope.drawRoute(viewport: MapViewport, route: List<GeoPoint>, colors: WanderExtendedColors) {
    if (route.size < 2) return
    val path = Path().apply {
        route.forEachIndexed { index, point ->
            val p = viewport.project(point)
            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
    }
    drawPath(path, Color.White, style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, colors.mapRoute, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawUser(position: Offset, pulse: Float, colors: WanderExtendedColors) {
    val base = 9.dp.toPx()
    drawCircle(colors.brandTeal.copy(alpha = 0.35f * (1f - pulse)), radius = base + 26.dp.toPx() * pulse, center = position)
    drawCircle(Color.White, radius = base + 3.dp.toPx(), center = position)
    drawCircle(colors.brandTeal, radius = base, center = position)
}
