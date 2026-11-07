package com.vibra.wanderai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

/**
 * Brand slider: thick rounded track, white thumb with coloured ring that grows
 * while dragging, and min/max captions underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WanderSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
    accessibilityLabel: String,
    valueDescription: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    accent: Color = MaterialTheme.colorScheme.secondary,
) {
    val interaction = remember { MutableInteractionSource() }
    val dragging by interaction.collectIsDraggedAsState()
    val colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = accent,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            interactionSource = interaction,
            colors = colors,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = accessibilityLabel
                    stateDescription = valueDescription
                },
            thumb = {
                val size = if (dragging) 28.dp else 24.dp
                Box(
                    modifier = Modifier
                        .size(size)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(5.dp, accent, CircleShape),
                )
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null,
                    modifier = Modifier.height(8.dp),
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(startLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
