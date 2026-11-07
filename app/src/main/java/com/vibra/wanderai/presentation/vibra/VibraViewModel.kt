package com.vibra.wanderai.presentation.vibra

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.VibraLinkState
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.model.WearWatch
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import com.vibra.wanderai.domain.usecase.NavigationGuidanceController
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
import com.vibra.wanderai.domain.usecase.VibraLinkManager
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VibraUiState(
    val connection: VibraConnectionState = VibraConnectionState.Disconnected,
    val devices: List<VibraDevice> = emptyList(),
    val intensity: HapticIntensity = HapticIntensity.MEDIUM,
    val syncStatus: WatchSyncStatus = WatchSyncStatus.Idle,
    val lastEvent: HapticEvent? = null,
    val activeItinerary: Itinerary? = null,
    val bluetoothSupported: Boolean = true,
    val commandError: VibraError? = null,
    /** Vibra watches reachable through the Wear OS Data Layer. */
    val wearWatches: List<WearWatch> = emptyList(),
    /** Guidance paused by the traveler from the watch. */
    val guidancePaused: Boolean = false,
    /** BLE or Wear OS Data Layer link, whichever is available (see [VibraLinkManager]). */
    val link: VibraLinkState = VibraLinkState.Idle,
) {
    val isConnected: Boolean get() = connection is VibraConnectionState.Connected
    val isWearConnected: Boolean get() = wearWatches.isNotEmpty()

    /** True when cues can reach a watch over BLE, the simulator or Wear OS. */
    val canSendToWatch: Boolean get() = link is VibraLinkState.Linked || isConnected || isWearConnected
    val isBusy: Boolean
        get() = link is VibraLinkState.ScanningBle || link is VibraLinkState.Connecting ||
            link is VibraLinkState.CheckingDataLayer
}

class VibraViewModel(
    private val vibraRepository: VibraRepository,
    itineraryRepository: ItineraryRepository,
    private val syncItineraryToWatch: SyncItineraryToWatchUseCase,
    wearRepository: WearDataLayerRepository,
    private val guidance: NavigationGuidanceController,
    private val linkManager: VibraLinkManager,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val commandError = MutableStateFlow<VibraError?>(null)

    val state: StateFlow<VibraUiState> = combine(
        combine(vibraRepository.connectionState, vibraRepository.discoveredDevices, vibraRepository.hapticIntensity, ::Triple),
        combine(vibraRepository.syncStatus, vibraRepository.lastEvent, ::Pair),
        combine(wearRepository.connectedWatches, guidance.state, linkManager.state, ::Triple),
        itineraryRepository.activeItinerary,
        commandError,
    ) { (connection, devices, intensity), (sync, event), (watches, guidanceState, link), itinerary, error ->
        VibraUiState(
            connection = connection,
            devices = devices,
            intensity = intensity,
            syncStatus = sync,
            lastEvent = event,
            activeItinerary = itinerary,
            bluetoothSupported = vibraRepository.isBluetoothSupported(),
            commandError = error,
            wearWatches = watches,
            guidancePaused = guidanceState.paused,
            link = link,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VibraUiState())

    fun isBluetoothEnabled(): Boolean = vibraRepository.isBluetoothEnabled()

    /** "Buscar mi Vibra": BLE first, automatic fallback to Wear OS Data Layer nodes. */
    fun search() = linkManager.search()

    /** BLE cannot be used (permission or Bluetooth declined): check the Data Layer only. */
    fun searchDataLayerOnly() = linkManager.searchDataLayerOnly()

    fun stopScan() = vibraRepository.stopScan()

    fun connect(device: VibraDevice) = vibraRepository.connect(device)

    fun connectSimulator() = vibraRepository.connect(VibraDevice.SIMULATOR)

    fun disconnect() = vibraRepository.disconnect()

    fun setIntensity(intensity: HapticIntensity) = vibraRepository.setHapticIntensity(intensity)

    fun send(command: HapticCommand) {
        viewModelScope.launch {
            val wearSent = guidance.publish(command).isSuccess
            val bleSent = state.value.isConnected && vibraRepository.send(command).isSuccess
            commandError.value = when {
                wearSent || bleSent -> null
                state.value.canSendToWatch -> VibraError.WRITE_FAILED
                else -> VibraError.NOT_CONNECTED
            }
        }
    }

    fun syncItinerary() {
        val itinerary = state.value.activeItinerary ?: return
        viewModelScope.launch { syncItineraryToWatch(itinerary, location.value.point) }
    }
}
