package com.vibra.wanderai.data.wear

import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.WatchUserAction

/**
 * Contract between WanderAI (phone) and the Vibra Wear OS app over the Data Layer.
 *
 * Phone ➔ watch
 *  - [PATH_NAVIGATION_EVENT] (MessageClient), UTF-8 JSON:
 *    ```
 *    {"action":"U_TURN","instruction":"…","distance_meters":50,
 *     "vibration_pattern":{"pulses":3,"duration_ms":200,"amplitude":255},
 *     "timestamp":1778344620000}
 *    ```
 *  - [PATH_ACTIVE_ROUTE] (DataClient DataMap): [KEY_CURRENT_DESTINATION] (String),
 *    [KEY_REMAINING_DISTANCE] (String), [KEY_ROUTE_ACTIVE] (Boolean).
 *
 * Watch ➔ phone
 *  - [PATH_USER_ACTION] (MessageClient): the action name as plain UTF-8 text
 *    (`ACK`) or as JSON (`{"action":"ACK"}`).
 *
 * Both apps must share the same applicationId and signing key for the Data Layer
 * to deliver messages between them.
 */
object WearProtocol {
    const val PATH_NAVIGATION_EVENT = "/wanderai/navigation_event"
    const val PATH_ACTIVE_ROUTE = "/wanderai/active_route"
    const val PATH_USER_ACTION = "/vibra/user_action"

    const val KEY_CURRENT_DESTINATION = "current_destination"
    const val KEY_REMAINING_DISTANCE = "remaining_distance"
    const val KEY_ROUTE_ACTIVE = "route_active"

    /** Name the Vibra watch app uses for [WatchUserAction.ACK]. */
    private const val VIBRA_ACKNOWLEDGE = "ACKNOWLEDGE"

    private val ACTION_FIELD = Regex("\"action\"\\s*:\\s*\"([A-Za-z_]+)\"")

    fun encodeNavigationEvent(event: NavigationEvent): ByteArray = buildString {
        val pattern = event.vibrationPattern
        append("{\"action\":")
        appendJsonString(event.action.name)
        append(",\"instruction\":")
        appendJsonString(event.instruction)
        append(",\"distance_meters\":").append(event.distanceMeters)
        append(",\"vibration_pattern\":{")
        append("\"pulses\":").append(pattern.pulses)
        append(",\"duration_ms\":").append(pattern.durationMs)
        append(",\"amplitude\":").append(pattern.amplitude)
        append("},\"timestamp\":").append(event.timestampMillis)
        append('}')
    }.toByteArray(Charsets.UTF_8)

    /** Returns null for payloads that do not name a known [WatchUserAction]. */
    fun decodeUserAction(payload: ByteArray): WatchUserAction? {
        val text = payload.toString(Charsets.UTF_8).trim()
        val name = if (text.startsWith("{")) {
            ACTION_FIELD.find(text)?.groupValues?.get(1)
        } else {
            text.trim('"')
        } ?: return null
        val normalized = name.trim().let { if (it.equals(VIBRA_ACKNOWLEDGE, ignoreCase = true)) WatchUserAction.ACK.name else it }
        return WatchUserAction.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
            }
        }
        append('"')
    }
}
