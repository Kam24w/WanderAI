package com.vibra.wanderai.presentation.vibra

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
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
) {
    val isConnected: Boolean get() = connection is VibraConnectionState.Connected
    val isBusy: Boolean get() = connection is VibraConnectionState.Scanning || connection is VibraConnectionState.Connecting
}

class VibraViewModel(
    private val vibraRepository: VibraRepository,
    itineraryRepository: ItineraryRepository,
    private val syncItineraryToWatch: SyncItineraryToWatchUseCase,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val commandError = MutableStateFlow<VibraError?>(null)

    val state: StateFlow<VibraUiState> = combine(
        combine(vibraRepository.connectionState, vibraRepository.discoveredDevices, vibraRepository.hapticIntensity, ::Triple),
        combine(vibraRepository.syncStatus, vibraRepository.lastEvent, ::Pair),
        itineraryRepository.activeItinerary,
        commandError,
    ) { (connection, devices, intensity), (sync, event), itinerary, error ->
        VibraUiState(
            connection = connection,
            devices = devices,
            intensity = intensity,
            syncStatus = sync,
            lastEvent = event,
            activeItinerary = itinerary,
            bluetoothSupported = vibraRepository.isBluetoothSupported(),
            commandError = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VibraUiState())

    fun isBluetoothEnabled(): Boolean = vibraRepository.isBluetoothEnabled()

    fun startScan() = vibraRepository.startScan()

    fun stopScan() = vibraRepository.stopScan()

    fun connect(device: VibraDevice) = vibraRepository.connect(device)

    fun connectSimulator() = vibraRepository.connect(VibraDevice.SIMULATOR)

    fun disconnect() = vibraRepository.disconnect()

    fun setIntensity(intensity: HapticIntensity) = vibraRepository.setHapticIntensity(intensity)

    fun send(command: HapticCommand) {
        viewModelScope.launch {
            commandError.value = vibraRepository.send(command).exceptionOrNull()?.let { VibraError.WRITE_FAILED }
        }
    }

    fun syncItinerary() {
        val itinerary = state.value.activeItinerary ?: return
        viewModelScope.launch { syncItineraryToWatch(itinerary, location.value.point) }
    }
}
