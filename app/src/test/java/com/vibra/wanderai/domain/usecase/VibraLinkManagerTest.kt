package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.VibraLinkState
import com.vibra.wanderai.domain.model.VibraTransport
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.model.WatchUserAction
import com.vibra.wanderai.domain.model.WearWatch
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VibraLinkManagerTest {

    private class FakeBle(var supported: Boolean = true, var scanResult: VibraConnectionState? = null) : VibraRepository {
        override val connectionState = MutableStateFlow<VibraConnectionState>(VibraConnectionState.Disconnected)
        override val discoveredDevices = MutableStateFlow<List<VibraDevice>>(emptyList())
        override val hapticIntensity = MutableStateFlow(HapticIntensity.MEDIUM)
        override val syncStatus = MutableStateFlow<WatchSyncStatus>(WatchSyncStatus.Idle)
        override val lastEvent = MutableStateFlow<HapticEvent?>(null)
        var scans = 0

        override fun isBluetoothSupported() = supported
        override fun isBluetoothEnabled() = supported
        override fun startScan() {
            scans++
            connectionState.value = scanResult ?: VibraConnectionState.Scanning
        }
        override fun stopScan() {
            if (connectionState.value == VibraConnectionState.Scanning) connectionState.value = VibraConnectionState.Disconnected
        }
        override fun connect(device: VibraDevice) {
            connectionState.value = VibraConnectionState.Connected(device, batteryPercent = 80)
        }
        override fun disconnect() { connectionState.value = VibraConnectionState.Disconnected }
        override fun setHapticIntensity(intensity: HapticIntensity) = Unit
        override suspend fun send(command: HapticCommand) = Result.success(Unit)
        override fun updateSyncStatus(status: WatchSyncStatus) = Unit
    }

    private class FakeWear(var nodes: List<WearWatch> = emptyList()) : WearDataLayerRepository {
        override val connectedWatches = MutableStateFlow<List<WearWatch>>(emptyList())
        override val userActions = MutableSharedFlow<WatchUserAction>()
        var queries = 0

        override suspend fun refreshConnectedWatches(): List<WearWatch> {
            queries++
            connectedWatches.value = nodes
            return nodes
        }
        override suspend fun sendNavigationEvent(event: NavigationEvent) = Result.success(1)
        override suspend fun updateActiveRoute(route: ActiveRouteState) = Result.success(Unit)
    }

    private val emulator = WearWatch("emu-1", "Wear OS Emulator")

    private fun TestScope.manager(ble: FakeBle, wear: FakeWear): VibraLinkManager {
        val manager = VibraLinkManager(ble, wear, backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { manager.state.collect {} }
        return manager
    }

    @Test
    fun `BLE timeout falls back to the Data Layer and links the emulator`() = runTest(UnconfinedTestDispatcher()) {
        val ble = FakeBle()
        val wear = FakeWear(nodes = listOf(emulator))
        val manager = manager(ble, wear)

        manager.search()
        assertEquals(VibraLinkState.ScanningBle, manager.state.value)
        assertEquals(0, wear.queries)

        ble.connectionState.value = VibraConnectionState.Failed(VibraError.NO_DEVICES_FOUND) // scan timed out

        assertEquals(1, wear.queries)
        assertEquals(VibraLinkState.Linked(VibraTransport.DATA_LAYER, "Wear OS Emulator", null), manager.state.value)
    }

    @Test
    fun `no BLE radio goes straight to the Data Layer`() = runTest(UnconfinedTestDispatcher()) {
        val ble = FakeBle(supported = false)
        val wear = FakeWear(nodes = listOf(emulator))
        val manager = manager(ble, wear)

        manager.search()

        assertEquals(0, ble.scans)
        assertEquals(VibraTransport.DATA_LAYER, (manager.state.value as VibraLinkState.Linked).transport)
    }

    @Test
    fun `Bluetooth disabled triggers the fallback`() = runTest(UnconfinedTestDispatcher()) {
        val ble = FakeBle(scanResult = VibraConnectionState.Failed(VibraError.BLUETOOTH_DISABLED))
        val wear = FakeWear(nodes = listOf(emulator))
        val manager = manager(ble, wear)

        manager.search()

        assertEquals(1, wear.queries)
        assertEquals(VibraTransport.DATA_LAYER, (manager.state.value as VibraLinkState.Linked).transport)
    }

    @Test
    fun `nothing on either link reports NO_WATCH_FOUND`() = runTest(UnconfinedTestDispatcher()) {
        val ble = FakeBle()
        val manager = manager(ble, FakeWear())

        manager.search()
        ble.connectionState.value = VibraConnectionState.Failed(VibraError.NO_DEVICES_FOUND)

        assertEquals(VibraLinkState.Failed(VibraError.NO_WATCH_FOUND), manager.state.value)
    }

    @Test
    fun `a physical watch found over BLE wins and skips the fallback`() = runTest(UnconfinedTestDispatcher()) {
        val ble = FakeBle()
        val wear = FakeWear(nodes = listOf(emulator))
        val manager = manager(ble, wear)

        manager.search()
        ble.connectionState.value = VibraConnectionState.Disconnected // scan ended with devices listed
        ble.connect(VibraDevice("AA:BB", "Vibra-01", rssi = -50))

        assertEquals(0, wear.queries)
        assertEquals(VibraLinkState.Linked(VibraTransport.BLE, "Vibra-01", 80), manager.state.value)
    }

    @Test
    fun `declined Bluetooth checks only the Data Layer`() = runTest(UnconfinedTestDispatcher()) {
        val ble = FakeBle()
        val wear = FakeWear(nodes = listOf(emulator))
        val manager = manager(ble, wear)

        manager.searchDataLayerOnly()

        assertEquals(0, ble.scans)
        assertEquals(VibraTransport.DATA_LAYER, (manager.state.value as VibraLinkState.Linked).transport)
    }
}
