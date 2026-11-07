package com.vibra.wanderai.presentation.screens.vibra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Battery4Bar
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.wanderai.R
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.GoldAccent
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand
import kotlinx.coroutines.delay

private enum class VibraState { Disconnected, Scanning, Connecting, Connected }
private enum class HapticLevel { Soft, Medium, Strong }

@Composable
fun VibraIntegrationScreen(onBackClick: () -> Unit) {
    var vibraState by rememberSaveable { mutableStateOf(VibraState.Disconnected) }
    var hapticLevel by rememberSaveable { mutableStateOf(HapticLevel.Medium) }

    LaunchedEffect(vibraState) {
        when (vibraState) {
            VibraState.Scanning -> {
                delay(2_500)
                vibraState = VibraState.Connecting
            }
            VibraState.Connecting -> {
                delay(1_500)
                vibraState = VibraState.Connected
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WanderTopBar(showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // â”€â”€ State header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            VibraStateHeader(state = vibraState)

            // â”€â”€ Description (shown when disconnected) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (vibraState == VibraState.Disconnected) {
                VibraDescriptionCard()
            }

            // â”€â”€ Scanning animation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (vibraState == VibraState.Scanning || vibraState == VibraState.Connecting) {
                VibraScanningCard(state = vibraState)
            }

            // â”€â”€ Watch Sync Status (shown when connected) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (vibraState == VibraState.Connected) {
                WatchSyncStatusCard()
                HapticControlCard(selected = hapticLevel, onSelect = { hapticLevel = it })
            }

            Spacer(Modifier.height(4.dp))

            // â”€â”€ Action button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            when (vibraState) {
                VibraState.Disconnected -> Button(
                    onClick = { vibraState = VibraState.Scanning },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.BluetoothSearching, null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.vibra_connect), color = Color.White,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }

                VibraState.Scanning, VibraState.Connecting -> OutlinedButton(
                    onClick = { vibraState = VibraState.Disconnected },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.back_button),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium)
                }

                VibraState.Connected -> OutlinedButton(
                    onClick = { vibraState = VibraState.Disconnected },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.vibra_disconnect),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// â”€â”€ State header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun VibraStateHeader(state: VibraState) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )

    val isAnimating = state == VibraState.Scanning || state == VibraState.Connecting
    val ringColor by animateColorAsState(
        targetValue = when (state) {
            VibraState.Connected -> TealBrand
            VibraState.Disconnected -> MaterialTheme.colorScheme.outlineVariant
            else -> NavyDeep
        }, label = "ring"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .then(if (isAnimating) Modifier.scale(scale) else Modifier)
                .clip(CircleShape)
                .background(
                    when (state) {
                        VibraState.Connected -> TealBrand.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(2.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (state) {
                    VibraState.Scanning -> Icons.AutoMirrored.Outlined.BluetoothSearching
                    VibraState.Connecting -> Icons.Outlined.Bluetooth
                    VibraState.Connected -> Icons.Outlined.Watch
                    else -> Icons.Outlined.Watch
                },
                contentDescription = null,
                tint = when (state) {
                    VibraState.Connected -> TealBrand
                    VibraState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> NavyDeep
                },
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.vibra_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = when (state) {
                VibraState.Disconnected -> stringResource(R.string.vibra_disconnected)
                VibraState.Scanning -> stringResource(R.string.vibra_scanning)
                VibraState.Connecting -> stringResource(R.string.vibra_connecting)
                VibraState.Connected -> stringResource(R.string.vibra_connected)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when (state) {
                VibraState.Connected -> TealBrand
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (state == VibraState.Connected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// â”€â”€ Description card (disconnected) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun VibraDescriptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = NavyDeep.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(NavyDeep.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Vibration, null, tint = NavyDeep, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.vibra_subtitle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.vibra_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// â”€â”€ Scanning card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun VibraScanningCard(state: VibraState) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmer.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.BluetoothSearching,
                    null,
                    tint = NavyDeep.copy(alpha = alpha),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state == VibraState.Scanning)
                        stringResource(R.string.vibra_scanning)
                    else stringResource(R.string.vibra_connecting),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state == VibraState.Connecting) {
                DeviceFoundRow()
            } else {
                repeat(2) { idx ->
                    DevicePlaceholderRow(alpha = if (idx == 0) alpha else alpha * 0.6f)
                    if (idx == 0) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DevicePlaceholderRow(alpha: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .padding(12.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Box(
                Modifier
                    .width(120.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(80.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha * 0.6f))
            )
        }
    }
}

@Composable
private fun DeviceFoundRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(TealBrand.copy(alpha = 0.08f))
            .border(1.dp, TealBrand.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TealBrand.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Watch, null, tint = TealBrand, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Vibra Gen 2", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Conectandoâ€¦", style = MaterialTheme.typography.bodySmall,
                color = TealBrand)
        }
        Icon(Icons.Outlined.Bluetooth, null, tint = TealBrand, modifier = Modifier.size(18.dp))
    }
}

// â”€â”€ Watch Sync Status card (connected) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun WatchSyncStatusCard(compact: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(if (compact) 14.dp else 20.dp)) {

            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(TealBrand)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.vibra_sync_status),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Battery4Bar, null, tint = TealBrand,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("84%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Watch, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "WanderAI â†’ Vibra Gen 2",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Route info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.horizontalGradient(
                            listOf(NavyDeep.copy(alpha = 0.08f), TealBrand.copy(alpha = 0.06f))
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, tint = GoldAccent,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Plz. Mayor Â· Pasto",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SyncInfoChip(label = "A 90 m", icon = Icons.Outlined.NearMe, color = TealBrand)
                        SyncInfoChip(label = "Sincronizado", icon = Icons.Outlined.CheckCircle, color = TealBrand)
                        SyncInfoChip(label = "70 m a pie", icon = Icons.AutoMirrored.Outlined.ArrowForward, color = NavyDeep)
                    }
                }
            }

            if (!compact) {
                Spacer(Modifier.height(14.dp))

                // Haptic instruction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(GoldAccent.copy(alpha = 0.08f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Vibration, null, tint = GoldAccent,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.vibra_arrow_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncInfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.Medium)
    }
}

// â”€â”€ Haptic control card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun HapticControlCard(selected: HapticLevel, onSelect: (HapticLevel) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Vibration, null, tint = TealBrand, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.vibra_haptic_intensity),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    HapticLevel.Soft to stringResource(R.string.vibra_haptic_soft),
                    HapticLevel.Medium to stringResource(R.string.vibra_haptic_medium),
                    HapticLevel.Strong to stringResource(R.string.vibra_haptic_strong)
                ).forEach { (level, label) ->
                    val isSelected = selected == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isSelected) TealBrand else MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (!isSelected) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.medium
                                ) else Modifier
                            )
                            .padding(vertical = 12.dp)
                            .let { mod ->
                                mod.then(Modifier)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Wrapping in a clickable surface
                        androidx.compose.material3.Surface(
                            onClick = { onSelect(level) },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
