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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.presentation.common.color
import com.vibra.wanderai.presentation.common.formatCopShort
import com.vibra.wanderai.presentation.common.formatDistance
import com.vibra.wanderai.presentation.common.icon
import com.vibra.wanderai.presentation.common.labelRes
import com.vibra.wanderai.presentation.common.localized
import com.vibra.wanderai.presentation.common.messageRes
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
            it.syncItineraryToWatch, it.observeTravelerLocation,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val viewport = rememberMapViewport()
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

    val route = remember(state.activeItinerary, state.location.point) {
        state.activeItinerary?.let { itinerary ->
            listOf(state.location.point) + itinerary.stops.map { it.place.location }
        }.orEmpty()
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
            PastoMap(
                viewport = viewport,
                places = state.visiblePlaces,
                highlightedPlaceIds = if (state.activeItinerary != null) state.recommendedIds else emptySet(),
                route = route,
                userLocation = state.location.point,
                selectedPlaceId = state.selectedPlaceId,
                onPlaceClick = { viewModel.selectPlace(it.id) },
                onMapClick = { viewModel.selectPlace(null) },
                mapDescription = stringResource(R.string.explore_map_description),
                modifier = Modifier.fillMaxSize(),
            )

            // Floating controls (right).
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MapFab(Icons.Outlined.MyLocation, stringResource(R.string.explore_my_location)) {
                    if (!locationPermission.isGranted) locationPermission.request()
                    scope.launch { viewport.animateTo(state.location.point, targetScale = 1.6f) }
                }
                MapFab(Icons.Outlined.Add, stringResource(R.string.explore_zoom_in)) { viewport.zoomBy(1.4f) }
                MapFab(Icons.Outlined.Remove, stringResource(R.string.explore_zoom_out)) { viewport.zoomBy(1 / 1.4f) }
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
                    selected = state.showOnlyRecommended,
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
                            onClose = { viewModel.selectPlace(null) },
                        )
                    }
                }
                WatchSyncCard(
                    state = state,
                    onConnect = onOpenVibra,
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

@Composable
private fun PlaceDetailCard(place: Place, distanceMeters: Int, onClose: () -> Unit) {
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
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Navy floating card from the reference: "WATCH SYNC STATUS · WanderAI → Vibra". */
@Composable
private fun WatchSyncCard(
    state: ExploreUiState,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onGenerate: () -> Unit,
) {
    val colors = WanderTheme.colors
    val connected = state.connection is VibraConnectionState.Connected
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
        !connected -> stringResource(R.string.sync_msg_disconnected)
        sync is WatchSyncStatus.Sending -> stringResource(R.string.sync_msg_sending)
        state.isActiveItinerarySynced && sync is WatchSyncStatus.Synced ->
            stringResource(R.string.sync_msg_synced, sync.firstStopWalkingMinutes, sync.firstStopName)
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
                !connected -> Triple(stringResource(R.string.sync_action_connect), onConnect, Icons.Outlined.Watch)
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
