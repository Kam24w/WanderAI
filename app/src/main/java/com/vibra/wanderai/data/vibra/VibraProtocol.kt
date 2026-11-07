package com.vibra.wanderai.data.vibra

import android.Manifest
import android.os.Build
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.TurnDirection
import java.util.UUID

/**
 * Wire protocol between WanderAI and the Vibra watch firmware.
 *
 * Transport: BLE GATT using the Nordic UART Service layout (widely supported
 * by nRF52 / ESP32 firmware). Each command is a 4-byte frame written to the
 * RX characteristic:
 *
 * ```
 * [0] opcode   [1] intensity (1..3)   [2..3] value, unsigned big-endian
 * ```
 * `value` is metres for navigation cues and the stop count for a sync header.
 * Battery is read from the standard Battery Service (0x180F / 0x2A19).
 */
object VibraProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val HAPTIC_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    const val DEVICE_NAME_PREFIX = "Vibra"

    private const val OP_TURN_LEFT: Byte = 0x01
    private const val OP_TURN_RIGHT: Byte = 0x02
    private const val OP_STRAIGHT: Byte = 0x03
    private const val OP_ARRIVED: Byte = 0x04
    private const val OP_SYNC: Byte = 0x10
    private const val OP_PING: Byte = 0x7F

    /** Runtime permissions needed to scan and connect. */
    val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun encode(command: HapticCommand, intensity: HapticIntensity): ByteArray {
        val (opcode, value) = when (command) {
            is HapticCommand.Turn ->
                (if (command.direction == TurnDirection.LEFT) OP_TURN_LEFT else OP_TURN_RIGHT) to command.distanceMeters
            is HapticCommand.Straight -> OP_STRAIGHT to command.distanceMeters
            HapticCommand.Arrived -> OP_ARRIVED to 0
            is HapticCommand.ItinerarySync -> OP_SYNC to command.stopCount
            HapticCommand.Ping -> OP_PING to 0
        }
        val clamped = value.coerceIn(0, 0xFFFF)
        return byteArrayOf(
            opcode,
            intensity.level.toByte(),
            (clamped shr 8 and 0xFF).toByte(),
            (clamped and 0xFF).toByte(),
        )
    }

    /**
     * Vibration pattern the firmware plays for each cue; reused by the on-phone
     * simulator. Returns alternating off/on timings (ms) and the amplitude (1..255).
     */
    fun waveform(command: HapticCommand, intensity: HapticIntensity): Pair<LongArray, Int> {
        val timings = when (command) {
            is HapticCommand.Turn ->
                if (command.direction == TurnDirection.LEFT) longArrayOf(0, 120, 90, 120)
                else longArrayOf(0, 120, 90, 120, 90, 120)
            is HapticCommand.Straight -> longArrayOf(0, 350)
            HapticCommand.Arrived -> longArrayOf(0, 80, 60, 80, 60, 80, 60, 400)
            is HapticCommand.ItinerarySync -> longArrayOf(0, 60, 80, 220)
            HapticCommand.Ping -> longArrayOf(0, 90)
        }
        val amplitude = when (intensity) {
            HapticIntensity.SOFT -> 90
            HapticIntensity.MEDIUM -> 170
            HapticIntensity.STRONG -> 255
        }
        return timings to amplitude
    }
}
