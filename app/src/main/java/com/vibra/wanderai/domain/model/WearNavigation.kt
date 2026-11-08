package com.vibra.wanderai.domain.model

/** Cue types understood by the Vibra Wear OS app on `/wanderai/navigation_event`. */
enum class NavigationAction { TURN_LEFT, TURN_RIGHT, U_TURN, STRAIGHT, ARRIVED, ALERT }

/** How the watch should buzz for a cue: [pulses] of [durationMs] each at [amplitude] (1..255). */
data class VibrationPattern(
    val pulses: Int,
    val durationMs: Int,
    val amplitude: Int,
) {
    companion object {
        private const val MAX_AMPLITUDE = 255

        /** Pulse counts mirror the BLE firmware patterns (2 = left, 3 = right, 1 long = straight). */
        fun forAction(action: NavigationAction, intensity: HapticIntensity): VibrationPattern =
            when (action) {
                NavigationAction.TURN_LEFT -> VibrationPattern(2, 120, intensity.amplitude)
                NavigationAction.TURN_RIGHT -> VibrationPattern(3, 120, intensity.amplitude)
                NavigationAction.U_TURN -> VibrationPattern(3, 200, intensity.amplitude)
                NavigationAction.STRAIGHT -> VibrationPattern(1, 350, intensity.amplitude)
                NavigationAction.ARRIVED -> VibrationPattern(4, 150, intensity.amplitude)
                // Alerts ignore the user's intensity so they are never missed.
                NavigationAction.ALERT -> VibrationPattern(5, 100, MAX_AMPLITUDE)
            }
    }
}

/** One navigation cue sent phone ➔ watch. */
data class NavigationEvent(
    val action: NavigationAction,
    val instruction: String,
    val distanceMeters: Int,
    val vibrationPattern: VibrationPattern,
    val timestampMillis: Long,
)

/** Persistent route summary mirrored to the watch on `/wanderai/active_route`. */
data class ActiveRouteState(
    val currentDestination: String,
    val remainingDistance: String,
    val routeActive: Boolean,
) {
    companion object {
        val INACTIVE = ActiveRouteState(currentDestination = "", remainingDistance = "", routeActive = false)
    }
}

/** Actions the traveler triggers from the wrist (`/vibra/user_action`). */
enum class WatchUserAction { ACK, REPEAT_INSTRUCTION, PAUSE_GUIDANCE, RESUME_GUIDANCE, STOP_NAVIGATION }

/** A Wear OS node currently reachable through the Data Layer. */
data class WearWatch(val id: String, val displayName: String)

data class GuidanceState(
    /** True after the traveler sends PAUSE_GUIDANCE from the watch. */
    val paused: Boolean = false,
    /** Last cue generated (sent, or held while paused). */
    val lastEvent: NavigationEvent? = null,
    /** When the watch last confirmed a cue with ACK. */
    val lastAckMillis: Long? = null,
)

enum class VibraTransport { BLE, SIMULATOR, DATA_LAYER }

/** Combined BLE + Wear OS Data Layer link state shown by "Buscar mi Vibra". */
sealed interface VibraLinkState {
    data object Idle : VibraLinkState
    data object ScanningBle : VibraLinkState
    data class Connecting(val device: VibraDevice) : VibraLinkState
    /** BLE found nothing (or is unavailable): checking Wear OS Data Layer nodes. */
    data object CheckingDataLayer : VibraLinkState
    data class Linked(val transport: VibraTransport, val name: String, val batteryPercent: Int?) : VibraLinkState
    data class Failed(val error: VibraError) : VibraLinkState
}
