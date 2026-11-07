package com.vibra.wanderai.presentation.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlin.math.abs
import kotlin.math.ceil

/*
 * Single-series charts (the card title names the series → no legend box).
 * Specs: 2dp line, 10 % area wash, ≥8dp end-dot with a 2dp surface ring,
 * bars ≤24dp with 4dp rounded data-end, hairline solid grid, values in text
 * tokens only on the extreme / last point; tap reveals any value. The full
 * data set is exposed to TalkBack through [accessibilityDescription].
 */

private val PLOT_TOP = 22.dp
private val PLOT_BOTTOM = 22.dp
private val Y_AXIS_WIDTH = 32.dp

@Composable
fun AreaLineChart(
    values: List<Float>,
    labels: List<String>,
    formatValue: (Float) -> String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
    color: Color = WanderTheme.colors.brandTeal,
    height: Dp = 150.dp,
) {
    if (values.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val surface = MaterialTheme.colorScheme.surface
    val grid = WanderTheme.colors.chartGrid
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = muted)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = ink, fontWeight = FontWeight.Bold)
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    val maxValue = niceMax(values.max())
    val emphasized = selected ?: values.indices.maxBy { values[it] }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = accessibilityDescription }
            .pointerInput(values) {
                detectTapGestures { tap ->
                    val left = Y_AXIS_WIDTH.toPx()
                    val step = (size.width - left) / (values.size - 1).coerceAtLeast(1)
                    selected = values.indices.minBy { abs(left + it * step - tap.x) }
                }
            },
    ) {
        val left = Y_AXIS_WIDTH.toPx()
        val top = PLOT_TOP.toPx()
        val bottom = size.height - PLOT_BOTTOM.toPx()
        val plotHeight = bottom - top
        val step = (size.width - left) / (values.size - 1).coerceAtLeast(1)
        fun point(i: Int) = Offset(left + i * step, bottom - plotHeight * (values[i] / maxValue) * reveal.value)

        drawGrid(left, top, bottom, maxValue, grid, measurer, axisStyle, formatValue)

        val line = Path().apply {
            values.indices.forEach { i -> point(i).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(point(values.lastIndex).x, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(area, color.copy(alpha = 0.10f))
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // X labels
        labels.forEachIndexed { i, label ->
            val layout = measurer.measure(label, axisStyle)
            drawText(layout, topLeft = Offset(left + i * step - layout.size.width / 2f, bottom + 4.dp.toPx()))
        }

        // Emphasised point: extreme by default, or the tapped one.
        val p = point(emphasized)
        if (selected != null) {
            drawLine(grid, Offset(p.x, top), Offset(p.x, bottom), 1.dp.toPx())
        }
        drawCircle(surface, radius = 6.dp.toPx(), center = p)
        drawCircle(color, radius = 4.dp.toPx(), center = p)
        val text = measurer.measure(formatValue(values[emphasized]), valueStyle)
        val x = (p.x - text.size.width / 2f).coerceIn(left, size.width - text.size.width)
        drawText(text, topLeft = Offset(x, (p.y - text.size.height - 6.dp.toPx()).coerceAtLeast(0f)))
    }
}

@Composable
fun ColumnChart(
    values: List<Float>,
    labels: List<String>,
    formatValue: (Float) -> String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 150.dp,
) {
    if (values.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val grid = WanderTheme.colors.chartGrid
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = muted)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = ink, fontWeight = FontWeight.Bold)
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    val maxValue = niceMax(values.max())
    val emphasized = selected ?: values.lastIndex

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = accessibilityDescription }
            .pointerInput(values) {
                detectTapGestures { tap ->
                    val left = Y_AXIS_WIDTH.toPx()
                    val band = (size.width - left) / values.size
                    selected = ((tap.x - left) / band).toInt().coerceIn(0, values.lastIndex)
                }
            },
    ) {
        val left = Y_AXIS_WIDTH.toPx()
        val top = PLOT_TOP.toPx()
        val bottom = size.height - PLOT_BOTTOM.toPx()
        val plotHeight = bottom - top
        val band = (size.width - left) / values.size
        val barWidth = minOf(24.dp.toPx(), band * 0.6f)
        val radius = 4.dp.toPx()

        drawGrid(left, top, bottom, maxValue, grid, measurer, axisStyle, formatValue)

        values.forEachIndexed { i, value ->
            val cx = left + band * i + band / 2f
            val h = plotHeight * (value / maxValue) * reveal.value
            val active = i == emphasized
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = cx - barWidth / 2f, top = bottom - h, right = cx + barWidth / 2f, bottom = bottom,
                        topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                        bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero,
                    )
                )
            }
            drawPath(path, if (active) color else color.copy(alpha = 0.45f))

            val label = measurer.measure(labels.getOrElse(i) { "" }, axisStyle)
            drawText(label, topLeft = Offset(cx - label.size.width / 2f, bottom + 4.dp.toPx()))

            if (active) {
                val text = measurer.measure(formatValue(value), valueStyle)
                val x = (cx - text.size.width / 2f).coerceIn(left, size.width - text.size.width)
                drawText(text, topLeft = Offset(x, (bottom - h - text.size.height - 4.dp.toPx()).coerceAtLeast(0f)))
            }
        }
    }
}

/** Baseline + mid + top hairlines with clean tick values on a single axis. */
private fun DrawScope.drawGrid(
    left: Float,
    top: Float,
    bottom: Float,
    maxValue: Float,
    color: Color,
    measurer: TextMeasurer,
    style: TextStyle,
    format: (Float) -> String,
) {
    listOf(0f, 0.5f, 1f).forEach { fraction ->
        val y = bottom - (bottom - top) * fraction
        drawLine(color, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        val tick = measurer.measure(format(maxValue * fraction), style)
        drawText(tick, topLeft = Offset(0f, y - tick.size.height / 2f))
    }
}

/** Rounds the axis maximum up to a clean, even number. */
private fun niceMax(value: Float): Float {
    if (value <= 0f) return 1f
    val magnitude = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(value.toDouble()))).toFloat()
    val step = magnitude / 2f
    return (ceil(value / step) * step).let { if (it == value) it + step else it }
}
