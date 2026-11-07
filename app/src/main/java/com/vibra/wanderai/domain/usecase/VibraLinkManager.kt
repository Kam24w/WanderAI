package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.VibraLinkState
import com.vibra.wanderai.domain.model.VibraTransport
import com.vibra.wanderai.domain.model.WearWatch
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dual connectivity bridge for "Buscar mi Vibra":
 *  1. BLE scan for the physical watch (primary path, unchanged for production).
 *  2. If the scan times out without devices, or BLE is missing/off/denied, it falls
 *     back to the Wear OS Data Layer and checks connected nodes. That also detects a
 *     Wear OS emulator paired through the ADB tunnel (`adb forward tcp:5601 tcp:5601`).
 *
 * [state] merges both links; a reachable Data Layer node counts as connected.
 */
class VibraLinkManager(
    private val ble: VibraRepository,
    private val wear: WearDataLayerRepository,
    private val scope: CoroutineScope,
) {
    private enum class Fallback { NONE, CHECKING, NOT_FOUND }

    private val fallback = MutableStateFlow(Fallback.NONE)
    private var searchJob: Job? = null

    val state: StateFlow<VibraLinkState> =
        combine(ble.connectionState, wear.connectedWatches, fallback, ::resolve)
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), VibraLinkState.Idle)

    /** BLE first; the Data Layer is checked automatically if BLE cannot find a watch. */
    fun search() {
        searchJob?.cancel()
        fallback.value = Fallback.NONE
        if (!ble.isBluetoothSupported()) {
            searchJob = scope.launch { checkDataLayer() }
            return
        }
        ble.startScan() // Sets Scanning, or Failed right away when BLE is not usable.
        searchJob = scope.launch {
            val outcome = ble.connectionState.first { it !is VibraConnectionState.Scanning }
            if (outcome is VibraConnectionState.Failed && outcome.error in FALLBACK_ERRORS) checkDataLayer()
        }
    }

    /** Skips BLE (e.g. the user declined the permission or the "turn on Bluetooth" dialog). */
    fun searchDataLayerOnly() {
        searchJob?.cancel()
        ble.stopScan()
        searchJob = scope.launch { checkDataLayer() }
    }

    private suspend fun checkDataLayer() {
        fallback.value = Fallback.CHECKING
        val watches = wear.refreshConnectedWatches()
        fallback.value = if (watches.isEmpty()) Fallback.NOT_FOUND else Fallback.NONE
    }

    private fun resolve(
        connection: VibraConnectionState,
        watches: List<WearWatch>,
        fallback: Fallback,
    ): VibraLinkState = when {
        connection is VibraConnectionState.Connected -> VibraLinkState.Linked(
            transport = if (connection.device.isSimulated) VibraTransport.SIMULATOR else VibraTransport.BLE,
            name = connection.device.name,
            batteryPercent = connection.batteryPercent,
        )
        connection is VibraConnectionState.Connecting -> VibraLinkState.Connecting(connection.device)
        connection is VibraConnectionState.Scanning -> VibraLinkState.ScanningBle
        fallback == Fallback.CHECKING -> VibraLinkState.CheckingDataLayer
        watches.isNotEmpty() -> VibraLinkState.Linked(
            transport = VibraTransport.DATA_LAYER,
            name = watches.first().displayName,
            batteryPercent = null,
        )
        fallback == Fallback.NOT_FOUND -> VibraLinkState.Failed(VibraError.NO_WATCH_FOUND)
        connection is VibraConnectionState.Failed -> VibraLinkState.Failed(connection.error)
        else -> VibraLinkState.Idle
    }

    private companion object {
        /** BLE outcomes after which the Data Layer is tried automatically. */
        val FALLBACK_ERRORS = setOf(
            VibraError.NO_DEVICES_FOUND,
            VibraError.BLUETOOTH_UNAVAILABLE,
            VibraError.BLUETOOTH_DISABLED,
            VibraError.PERMISSION_DENIED,
        )
    }
}
