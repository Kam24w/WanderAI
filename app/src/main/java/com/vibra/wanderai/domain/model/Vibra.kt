package com.vibra.wanderai.domain.model

data class VibraDevice(
    val address: String,
    val name: String,
    val rssi: Int?,
    /** True for the on-phone haptic simulator used when no physical watch is around. */
    val isSimulated: Boolean = false,
) {
    companion object {
        /** Virtual watch that plays the haptic patterns on the phone itself. */
        val SIMULATOR = VibraDevice(
            address = "simulator",
            name = "Vibra Simulator",
            rssi = null,
            isSimulated = true,
        )
    }
}

sealed interface VibraConnectionState {
    data object Disconnected : VibraConnectionState
    data object Scanning : VibraConnectionState
    data class Connecting(val device: VibraDevice) : VibraConnectionState
    data class Connected(val device: VibraDevice, val batteryPercent: Int?) : VibraConnectionState
    data class Failed(val error: VibraError) : VibraConnectionState
}

enum class VibraError {
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    PERMISSION_DENIED,
    NO_DEVICES_FOUND,
    CONNECTION_LOST,
    PROTOCOL_UNSUPPORTED,
    WRITE_FAILED,
    NOT_CONNECTED,
    /** Neither BLE nor the Wear OS Data Layer found a watch. */
    NO_WATCH_FOUND,
}

/** [level] is the BLE frame value; [amplitude] (1..255) drives the vibration motor. */
enum class HapticIntensity(val level: Int, val amplitude: Int) { SOFT(1, 90), MEDIUM(2, 170), STRONG(3, 255) }

enum class TurnDirection { LEFT, RIGHT }

/** Navigation instruction delivered to the wrist as a vibration pattern. */
sealed interface HapticCommand {
    data class Turn(val direction: TurnDirection, val distanceMeters: Int) : HapticCommand
    data class Straight(val distanceMeters: Int) : HapticCommand
    data object Arrived : HapticCommand
    /** Turn around: the traveler is walking away from the destination. */
    data object UTurn : HapticCommand
    data class ItinerarySync(val stopCount: Int, val firstStopMeters: Int) : HapticCommand
    data object Ping : HapticCommand
}

data class HapticEvent(
    val command: HapticCommand,
    val sentAtMillis: Long,
)

sealed interface WatchSyncStatus {
    data object Idle : WatchSyncStatus
    data object Sending : WatchSyncStatus
    data class Synced(
        val itineraryId: String,
        val firstStopName: String,
        val firstStopMeters: Int,
        val firstStopTravelMinutes: Int,
    ) : WatchSyncStatus
    data class Failed(val error: VibraError) : WatchSyncStatus
}
