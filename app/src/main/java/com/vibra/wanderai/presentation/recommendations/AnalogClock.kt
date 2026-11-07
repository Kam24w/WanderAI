package com.vibra.wanderai.presentation.recommendations

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Clock face from the reference: shows the selected duration as if read on a
 * wall clock (2h 30min → hands at 2:30) and sweeps the elapsed arc in teal.
 */
@Composable
fun AnalogClock(
    totalMinutes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
) {
    val minuteAngle by animateFloatAsState(
        targetValue = (totalMinutes % 60) / 60f * 360f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "minuteHand",
    )
    val hourAngle by animateFloatAsState(
        targetValue = ((totalMinutes / 60f) % 12f) / 12f * 360f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hourHand",
    )

    val face = MaterialTheme.colorScheme.surfaceContainerHigh
    val rim = MaterialTheme.colorScheme.outlineVariant
    val ticks = MaterialTheme.colorScheme.onSurfaceVariant
    val hourHand = MaterialTheme.colorScheme.primary
    val accent = WanderTheme.colors.brandTeal
    val gold = WanderTheme.colors.brandGold

    Canvas(modifier = modifier.size(size)) {
        val radius = min(this.size.width, this.size.height) / 2f
        val c = center

        drawCircle(face, radius)
        drawCircle(rim, radius - 1.dp.toPx(), style = Stroke(2.dp.toPx()))

        // Elapsed sweep, capped at a full turn for > 12 h.
        val sweep = (totalMinutes.coerceAtMost(720) / 720f) * 360f
        drawArc(
            color = accent.copy(alpha = 0.18f),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = Offset(c.x - radius * 0.86f, c.y - radius * 0.86f),
            size = androidx.compose.ui.geometry.Size(radius * 1.72f, radius * 1.72f),
        )

        repeat(12) { i ->
            val major = i % 3 == 0
            val outer = radius * 0.88f
            val inner = radius * if (major) 0.70f else 0.78f
            drawLine(
                color = ticks.copy(alpha = if (major) 0.9f else 0.45f),
                start = c.polar(inner, i * 30f),
                end = c.polar(outer, i * 30f),
                strokeWidth = (if (major) 3.dp else 1.5.dp).toPx(),
                cap = StrokeCap.Round,
            )
        }

        hand(hourAngle, radius * 0.48f, 5.dp.toPx(), hourHand)
        hand(minuteAngle, radius * 0.72f, 3.5.dp.toPx(), accent)
        drawCircle(gold, 5.dp.toPx())
        drawCircle(Color.White, 2.dp.toPx())
    }
}

private fun DrawScope.hand(angle: Float, length: Float, width: Float, color: Color) {
    drawLine(color, center, center.polar(length, angle), strokeWidth = width, cap = StrokeCap.Round)
}

/** Point at [distance] from this offset, [degrees] clockwise from 12 o'clock. */
private fun Offset.polar(distance: Float, degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble() - 90.0)
    return Offset(x + distance * cos(rad).toFloat(), y + distance * sin(rad).toFloat())
}
