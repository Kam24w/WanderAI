package com.vibra.wanderai.presentation.vibra

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Battery5Bar
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Straight
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.presentation.common.describe
import com.vibra.wanderai.presentation.common.formatDistance
import com.vibra.wanderai.presentation.common.formatDuration
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.presentation.common.itineraryTitleRes
import com.vibra.wanderai.presentation.common.messageRes
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.AppPermissions
import com.vibra.wanderai.presentation.components.IconBadge
import com.vibra.wanderai.presentation.components.PrimaryButton
import com.vibra.wanderai.presentation.components.SectionCard
import com.vibra.wanderai.presentation.components.StatusPill
import com.vibra.wanderai.presentation.components.WanderCard
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.presentation.components.rememberPermissionState
import com.vibra.wanderai.ui.theme.WanderTheme

private const val TEST_DISTANCE_METERS = 50

@Composable
fun VibraScreen(onGenerateItinerary: () -> Unit) {
    val viewModel = wanderViewModel {
        VibraViewModel(it.vibraRepository, it.itineraryRepository, it.syncItineraryToWatch, it.observeTravelerLocation)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.startScan()
    }
    val bluetoothPermission = rememberPermissionState(AppPermissions.bluetooth) { granted ->
        if (granted) {
            if (viewModel.isBluetoothEnabled()) viewModel.startScan()
            else enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
    val startScan: () -> Unit = {
        when {
            !bluetoothPermission.isGranted -> bluetoothPermission.request()
            !viewModel.isBluetoothEnabled() -> enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> viewModel.startScan()
        }
    }
    LaunchedEffect(Unit) { viewModel.refreshLocation() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        WanderTopBar(title = stringResource(R.string.vibra_title))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ConnectionHero(state = state)

            when (val connection = state.connection) {
                is VibraConnectionState.Connected -> OutlinedButton(
                    onClick = viewModel::disconnect,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Outlined.LinkOff, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.vibra_disconnect), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                VibraConnectionState.Scanning -> PrimaryButton(
                    text = stringResource(R.string.vibra_stop_scan),
                    onClick = viewModel::stopScan,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                is VibraConnectionState.Connecting -> PrimaryButton(
                    text = stringResource(R.string.vibra_connecting),
                    onClick = {},
                    loading = true,
                )
                is VibraConnectionState.Failed -> {
                    ErrorBanner(connection.error)
                    ScanActions(
                        bluetoothSupported = state.bluetoothSupported,
                        onScan = startScan,
                        onSimulator = viewModel::connectSimulator,
                    )
                }
                VibraConnectionState.Disconnected -> ScanActions(
                    bluetoothSupported = state.bluetoothSupported,
                    onScan = startScan,
                    onSimulator = viewModel::connectSimulator,
                )
            }

            AnimatedVisibility(visible = !state.isConnected && state.devices.isNotEmpty()) {
                DeviceList(devices = state.devices, onConnect = viewModel::connect)
            }

            if (state.isConnected) {
                NavigationSyncCard(
                    state = state,
                    onSync = viewModel::syncItinerary,
                    onGenerate = onGenerateItinerary,
                )
                IntensityCard(selected = state.intensity, onSelect = viewModel::setIntensity)
                TestPadCard(state = state, onSend = viewModel::send)
            }

            HowItWorksCard()
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun ConnectionHero(state: VibraUiState) {
    val colors = WanderTheme.colors
    val connection = state.connection
    val (statusText, statusColor) = when (connection) {
        is VibraConnectionState.Connected -> stringResource(R.string.vibra_connected) to colors.success
        is VibraConnectionState.Connecting -> stringResource(R.string.vibra_connecting) to colors.brandGold
        VibraConnectionState.Scanning -> stringResource(R.string.vibra_scanning) to colors.brandTeal
        is VibraConnectionState.Failed -> stringResource(R.string.vibra_status_error) to MaterialTheme.colorScheme.error
        VibraConnectionState.Disconnected -> stringResource(R.string.vibra_disconnected) to Color.White.copy(alpha = 0.6f)
    }
    val device = when (connection) {
        is VibraConnectionState.Connected -> connection.device
        is VibraConnectionState.Connecting -> connection.device
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WatchIllustration(
                active = connection is VibraConnectionState.Connected,
                pulsing = state.isBusy,
                icon = state.lastEvent?.command?.icon ?: Icons.Outlined.Watch,
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vibra_hero_label),
                    color = colors.accentWarm,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = device?.name ?: stringResource(R.string.vibra_device_generic),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                StatusPill(text = statusText, color = statusColor, contentColor = Color.White, container = Color.White.copy(alpha = 0.12f))
                if (connection is VibraConnectionState.Connected) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        connection.batteryPercent?.let {
                            HeroMetric(Icons.Outlined.Battery5Bar, "$it%")
                        }
                        HeroMetric(
                            if (connection.device.isSimulated) Icons.Outlined.PhoneAndroid else Icons.Outlined.Bluetooth,
                            stringResource(if (connection.device.isSimulated) R.string.vibra_simulated else R.string.vibra_ble),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WatchIllustration(active: Boolean, pulsing: Boolean, icon: ImageVector) {
    val colors = WanderTheme.colors
    val transition = rememberInfiniteTransition(label = "watch")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        if (pulsing || active) {
            Canvas(Modifier.size(112.dp)) {
                val progress = if (pulsing) ring else 1f
                drawCircle(
                    color = (if (active) colors.success else colors.brandTeal).copy(alpha = if (pulsing) 0.5f * (1f - progress) else 0.25f),
                    radius = size.minDimension / 2f * (0.7f + 0.3f * progress),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        // Strap
        Box(
            Modifier
                .size(width = 34.dp, height = 104.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.18f))
        )
        // Body
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B1626))
                .border(3.dp, if (active) colors.brandTeal else Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (active) colors.accentWarm else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
        }
    }
}

// ── Discovery ─────────────────────────────────────────────────────────────────

@Composable
private fun ScanActions(bluetoothSupported: Boolean, onScan: () -> Unit, onSimulator: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bluetoothSupported) {
            PrimaryButton(
                text = stringResource(R.string.vibra_scan),
                icon = Icons.AutoMirrored.Outlined.BluetoothSearching,
                onClick = onScan,
            )
        }
        TextButton(onClick = onSimulator, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Vibration, null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.vibra_use_simulator), color = WanderTheme.colors.brandTeal, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorBanner(error: VibraError) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(error.messageRes), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DeviceList(devices: List<VibraDevice>, onConnect: (VibraDevice) -> Unit) {
    SectionCard(title = stringResource(R.string.vibra_devices_found), icon = Icons.Outlined.Bluetooth, iconTint = WanderTheme.colors.brandTeal) {
        devices.forEach { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClickLabel = stringResource(R.string.vibra_connect)) { onConnect(device) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(Icons.Outlined.Watch, tint = MaterialTheme.colorScheme.primary, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                device.rssi?.let {
                    Icon(Icons.Outlined.SignalCellularAlt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text("$it dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Connected controls ────────────────────────────────────────────────────────

@Composable
private fun NavigationSyncCard(state: VibraUiState, onSync: () -> Unit, onGenerate: () -> Unit) {
    val itinerary = state.activeItinerary
    val sync = state.syncStatus
    SectionCard(title = stringResource(R.string.vibra_nav_title), icon = Icons.Outlined.Sync, iconTint = WanderTheme.colors.accentWarm) {
        if (itinerary == null) {
            Text(stringResource(R.string.vibra_no_route), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = stringResource(R.string.sync_action_generate), icon = Icons.Outlined.AutoAwesome, onClick = onGenerate)
            return@SectionCard
        }
        Text(stringResource(itinerary.dominantCategory.itineraryTitleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            text = stringResource(R.string.vibra_route_summary, pluralStringResource(R.plurals.profile_stops, itinerary.stops.size, itinerary.stops.size), formatDuration(itinerary.totalMinutes), formatDistance(itinerary.totalDistanceMeters)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        val synced = sync is WatchSyncStatus.Synced && sync.itineraryId == itinerary.id
        if (sync is WatchSyncStatus.Synced && synced) {
            StatusPill(
                text = stringResource(R.string.sync_msg_synced, sync.firstStopWalkingMinutes, sync.firstStopName),
                color = WanderTheme.colors.success,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
        } else if (sync is WatchSyncStatus.Failed) {
            Text(stringResource(sync.error.messageRes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }
        PrimaryButton(
            text = stringResource(if (synced) R.string.sync_action_resend else R.string.vibra_send_route),
            icon = Icons.Outlined.Sync,
            loading = sync is WatchSyncStatus.Sending,
            containerColor = WanderTheme.colors.brandTeal,
            onClick = onSync,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntensityCard(selected: HapticIntensity, onSelect: (HapticIntensity) -> Unit) {
    SectionCard(title = stringResource(R.string.vibra_haptic_intensity), icon = Icons.Outlined.Vibration, iconTint = MaterialTheme.colorScheme.primary) {
        val options = HapticIntensity.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, intensity ->
                SegmentedButton(
                    selected = intensity == selected,
                    onClick = { onSelect(intensity) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = WanderTheme.colors.brandTeal,
                        activeContentColor = Color.White,
                    ),
                ) {
                    Text(
                        stringResource(
                            when (intensity) {
                                HapticIntensity.SOFT -> R.string.vibra_haptic_soft
                                HapticIntensity.MEDIUM -> R.string.vibra_haptic_medium
                                HapticIntensity.STRONG -> R.string.vibra_haptic_strong
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TestPadCard(state: VibraUiState, onSend: (HapticCommand) -> Unit) {
    SectionCard(title = stringResource(R.string.vibra_test_title), icon = Icons.Outlined.Watch, iconTint = WanderTheme.colors.brandGold) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestButton(Icons.Outlined.TurnLeft, stringResource(R.string.vibra_test_left), Modifier.weight(1f)) {
                onSend(HapticCommand.Turn(TurnDirection.LEFT, TEST_DISTANCE_METERS))
            }
            TestButton(Icons.Outlined.Straight, stringResource(R.string.vibra_test_straight), Modifier.weight(1f)) {
                onSend(HapticCommand.Straight(TEST_DISTANCE_METERS))
            }
            TestButton(Icons.Outlined.TurnRight, stringResource(R.string.vibra_test_right), Modifier.weight(1f)) {
                onSend(HapticCommand.Turn(TurnDirection.RIGHT, TEST_DISTANCE_METERS))
            }
            TestButton(Icons.Outlined.Flag, stringResource(R.string.vibra_test_arrived), Modifier.weight(1f)) {
                onSend(HapticCommand.Arrived)
            }
        }
        state.lastEvent?.let { event ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(event.command.icon, null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.vibra_last_command, event.command.describe()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.commandError?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TestButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun HowItWorksCard() {
    WanderCard {
        Text(stringResource(R.string.vibra_how_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.vibra_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        PatternLegend(Icons.Outlined.TurnLeft, stringResource(R.string.vibra_pattern_left))
        PatternLegend(Icons.Outlined.TurnRight, stringResource(R.string.vibra_pattern_right))
        PatternLegend(Icons.Outlined.Straight, stringResource(R.string.vibra_pattern_straight))
        PatternLegend(Icons.Outlined.Flag, stringResource(R.string.vibra_pattern_arrived))
    }
}

@Composable
private fun PatternLegend(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(WanderTheme.colors.brandTeal.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
