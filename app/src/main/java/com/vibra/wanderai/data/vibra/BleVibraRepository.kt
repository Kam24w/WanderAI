package com.vibra.wanderai.data.vibra

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.repository.VibraRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Bluetooth Low Energy link with the Vibra watch (see [VibraProtocol]).
 *
 * Also handles [VibraDevice.SIMULATOR], a virtual device that plays the same haptic
 * patterns on the phone's vibrator so the flow can be demoed without a watch.
 *
 * Every BLE call is guarded by [hasBluetoothPermissions]; lint's
 * MissingPermission is suppressed at class level for that reason.
 */
@SuppressLint("MissingPermission")
class BleVibraRepository(
    context: Context,
    private val scope: CoroutineScope,
) : VibraRepository {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }

    private val _connectionState = MutableStateFlow<VibraConnectionState>(VibraConnectionState.Disconnected)
    override val connectionState: StateFlow<VibraConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<VibraDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<VibraDevice>> = _discoveredDevices.asStateFlow()

    private val _hapticIntensity = MutableStateFlow(HapticIntensity.MEDIUM)
    override val hapticIntensity: StateFlow<HapticIntensity> = _hapticIntensity.asStateFlow()

    private val _syncStatus = MutableStateFlow<WatchSyncStatus>(WatchSyncStatus.Idle)
    override val syncStatus: StateFlow<WatchSyncStatus> = _syncStatus.asStateFlow()

    private val _lastEvent = MutableStateFlow<HapticEvent?>(null)
    override val lastEvent: StateFlow<HapticEvent?> = _lastEvent.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var hapticCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingWrite: CompletableDeferred<Boolean>? = null
    private var scanTimeoutJob: Job? = null
    private var userRequestedDisconnect = false
    private val writeMutex = Mutex()

    override fun isBluetoothSupported(): Boolean = adapter != null

    override fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    private fun hasBluetoothPermissions(): Boolean =
        VibraProtocol.requiredPermissions.all {
            ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
        }

    // ── Scan ──────────────────────────────────────────────────────────────────

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord
            val name = record?.deviceName ?: safeDeviceName(result.device)
            val advertisesService = record?.serviceUuids
                ?.any { it.uuid == VibraProtocol.SERVICE_UUID } == true
            val looksLikeVibra = name?.startsWith(VibraProtocol.DEVICE_NAME_PREFIX, ignoreCase = true) == true
            if (!advertisesService && !looksLikeVibra) return

            val device = VibraDevice(
                address = result.device.address,
                name = name ?: VibraProtocol.DEVICE_NAME_PREFIX,
                rssi = result.rssi,
            )
            _discoveredDevices.update { list ->
                (list.filterNot { it.address == device.address } + device).sortedByDescending { it.rssi ?: Int.MIN_VALUE }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            _connectionState.value = VibraConnectionState.Failed(VibraError.BLUETOOTH_UNAVAILABLE)
        }
    }

    override fun startScan() {
        val btAdapter = adapter
        val failure = when {
            btAdapter == null -> VibraError.BLUETOOTH_UNAVAILABLE
            !hasBluetoothPermissions() -> VibraError.PERMISSION_DENIED
            !btAdapter.isEnabled -> VibraError.BLUETOOTH_DISABLED
            else -> null
        }
        if (failure != null || btAdapter == null) {
            _connectionState.value = VibraConnectionState.Failed(failure ?: VibraError.BLUETOOTH_UNAVAILABLE)
            return
        }
        val scanner = btAdapter.bluetoothLeScanner ?: run {
            _connectionState.value = VibraConnectionState.Failed(VibraError.BLUETOOTH_DISABLED)
            return
        }

        _discoveredDevices.value = emptyList()
        _connectionState.value = VibraConnectionState.Scanning
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(null, settings, scanCallback) }
            .onFailure {
                Log.w(TAG, "startScan rejected", it)
                _connectionState.value = VibraConnectionState.Failed(VibraError.PERMISSION_DENIED)
                return
            }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(SCAN_DURATION_MS)
            stopScanInternal()
            if (_connectionState.value == VibraConnectionState.Scanning) {
                _connectionState.value = if (_discoveredDevices.value.isEmpty()) {
                    VibraConnectionState.Failed(VibraError.NO_DEVICES_FOUND)
                } else {
                    VibraConnectionState.Disconnected
                }
            }
        }
    }

    override fun stopScan() {
        scanTimeoutJob?.cancel()
        stopScanInternal()
        if (_connectionState.value == VibraConnectionState.Scanning) {
            _connectionState.value = VibraConnectionState.Disconnected
        }
    }

    private fun stopScanInternal() {
        if (!hasBluetoothPermissions()) return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
    }

    // ── Connection ────────────────────────────────────────────────────────────

    override fun connect(device: VibraDevice) {
        stopScan()
        closeGatt()
        userRequestedDisconnect = false
        _connectionState.value = VibraConnectionState.Connecting(device)

        if (device.isSimulated) {
            scope.launch {
                delay(SIMULATOR_HANDSHAKE_MS)
                _connectionState.value = VibraConnectionState.Connected(device, batteryPercent = SIMULATOR_BATTERY)
                playOnPhone(HapticCommand.Ping)
            }
            return
        }

        val btAdapter = adapter
        if (btAdapter == null || !hasBluetoothPermissions()) {
            _connectionState.value = VibraConnectionState.Failed(VibraError.PERMISSION_DENIED)
            return
        }
        val remote = runCatching { btAdapter.getRemoteDevice(device.address) }.getOrNull() ?: run {
            _connectionState.value = VibraConnectionState.Failed(VibraError.CONNECTION_LOST)
            return
        }
        @Suppress("DEPRECATION") // Replacement requires API 37; minSdk is 24.
        gatt = remote.connectGatt(appContext, false, GattCallback(device), BluetoothDevice.TRANSPORT_LE)
    }

    override fun disconnect() {
        userRequestedDisconnect = true
        closeGatt()
        _connectionState.value = VibraConnectionState.Disconnected
        _syncStatus.value = WatchSyncStatus.Idle
    }

    private fun closeGatt() {
        val current = gatt ?: return
        if (hasBluetoothPermissions()) {
            runCatching {
                current.disconnect()
                current.close()
            }
        }
        gatt = null
        hapticCharacteristic = null
        pendingWrite?.complete(false)
        pendingWrite = null
    }

    private inner class GattCallback(private val device: VibraDevice) : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when {
                newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS ->
                    gatt.discoverServices()

                newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS -> {
                    runCatching { gatt.close() }
                    if (this@BleVibraRepository.gatt == gatt) this@BleVibraRepository.gatt = null
                    hapticCharacteristic = null
                    if (!userRequestedDisconnect) {
                        _connectionState.value = VibraConnectionState.Failed(VibraError.CONNECTION_LOST)
                        _syncStatus.value = WatchSyncStatus.Idle
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val characteristic = gatt.getService(VibraProtocol.SERVICE_UUID)
                ?.getCharacteristic(VibraProtocol.HAPTIC_CHARACTERISTIC_UUID)
            if (status != BluetoothGatt.GATT_SUCCESS || characteristic == null) {
                userRequestedDisconnect = true
                closeGatt()
                _connectionState.value = VibraConnectionState.Failed(VibraError.PROTOCOL_UNSUPPORTED)
                return
            }
            hapticCharacteristic = characteristic
            _connectionState.value = VibraConnectionState.Connected(device, batteryPercent = null)

            gatt.getService(VibraProtocol.BATTERY_SERVICE_UUID)
                ?.getCharacteristic(VibraProtocol.BATTERY_LEVEL_UUID)
                ?.let { gatt.readCharacteristic(it) }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) = handleBattery(characteristic, value, status)

        @Deprecated("Called on API < 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) = handleBattery(characteristic, characteristic.value ?: ByteArray(0), status)

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        private fun handleBattery(characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            if (characteristic.uuid != VibraProtocol.BATTERY_LEVEL_UUID || value.isEmpty()) return
            _connectionState.update { state ->
                if (state is VibraConnectionState.Connected) {
                    state.copy(batteryPercent = value[0].toInt() and 0xFF)
                } else {
                    state
                }
            }
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    override fun setHapticIntensity(intensity: HapticIntensity) {
        _hapticIntensity.value = intensity
    }

    override fun updateSyncStatus(status: WatchSyncStatus) {
        _syncStatus.value = status
    }

    override suspend fun send(command: HapticCommand): Result<Unit> {
        val state = _connectionState.value as? VibraConnectionState.Connected
            ?: return Result.failure(IllegalStateException(VibraError.NOT_CONNECTED.name))

        val delivered = if (state.device.isSimulated) {
            playOnPhone(command)
            true
        } else {
            writeToWatch(VibraProtocol.encode(command, _hapticIntensity.value))
        }

        return if (delivered) {
            _lastEvent.value = HapticEvent(command, System.currentTimeMillis())
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(VibraError.WRITE_FAILED.name))
        }
    }

    private suspend fun writeToWatch(payload: ByteArray): Boolean = writeMutex.withLock {
        val currentGatt = gatt ?: return false
        val characteristic = hapticCharacteristic ?: return false
        if (!hasBluetoothPermissions()) return false

        val ack = CompletableDeferred<Boolean>()
        pendingWrite = ack
        val queued = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentGatt.writeCharacteristic(
                characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = payload
                currentGatt.writeCharacteristic(characteristic)
            }
        }
        val result = queued && (withTimeoutOrNull(WRITE_TIMEOUT_MS) { ack.await() } ?: false)
        pendingWrite = null
        result
    }

    private fun playOnPhone(command: HapticCommand) {
        val target = vibrator?.takeIf { it.hasVibrator() } ?: return
        val (timings, amplitude) = VibraProtocol.waveform(command, _hapticIntensity.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = IntArray(timings.size) { index -> if (index % 2 == 0) 0 else amplitude }
            target.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(timings, -1)
        }
    }

    private fun safeDeviceName(device: BluetoothDevice): String? =
        if (hasBluetoothPermissions()) runCatching { device.name }.getOrNull() else null

    companion object {
        private const val TAG = "VibraBle"
        private const val SCAN_DURATION_MS = 10_000L
        private const val WRITE_TIMEOUT_MS = 2_000L
        private const val SIMULATOR_HANDSHAKE_MS = 900L
        private const val SIMULATOR_BATTERY = 84
    }
}
