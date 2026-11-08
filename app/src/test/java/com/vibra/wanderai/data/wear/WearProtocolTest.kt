package com.vibra.wanderai.data.wear

import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.VibrationPattern
import com.vibra.wanderai.domain.model.WatchUserAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearProtocolTest {

    @Test
    fun `navigation event matches the watch contract`() {
        val event = NavigationEvent(
            action = NavigationAction.U_TURN,
            instruction = "Da la vuelta en U en 50 metros",
            distanceMeters = 50,
            vibrationPattern = VibrationPattern(pulses = 3, durationMs = 200, amplitude = 255),
            timestampMillis = 1_778_344_620_000,
        )

        val json = WearProtocol.encodeNavigationEvent(event).toString(Charsets.UTF_8)

        assertEquals(
            """{"action":"U_TURN","instruction":"Da la vuelta en U en 50 metros","distance_meters":50,""" +
                """"vibration_pattern":{"pulses":3,"duration_ms":200,"amplitude":255},"timestamp":1778344620000}""",
            json,
        )
    }

    @Test
    fun `instruction text is escaped and kept as UTF-8`() {
        val event = NavigationEvent(
            action = NavigationAction.ALERT,
            instruction = "Atención: \"Plaza\"\n\\ ñ",
            distanceMeters = 0,
            vibrationPattern = VibrationPattern(1, 1, 1),
            timestampMillis = 0,
        )

        val json = WearProtocol.encodeNavigationEvent(event).toString(Charsets.UTF_8)

        assertTrue(json.contains(""""instruction":"Atención: \"Plaza\"\n\\ ñ""""))
    }

    @Test
    fun `user actions decode from plain text or JSON`() {
        assertEquals(WatchUserAction.ACK, WearProtocol.decodeUserAction("ACK".toByteArray()))
        // Vibra (Wear OS) encodes UserAction as JSON with "ACKNOWLEDGE".
        assertEquals(
            WatchUserAction.ACK,
            WearProtocol.decodeUserAction("""{"action":"ACKNOWLEDGE","timestamp":1}""".toByteArray()),
        )
        assertEquals(WatchUserAction.PAUSE_GUIDANCE, WearProtocol.decodeUserAction(" pause_guidance\n".toByteArray()))
        assertEquals(
            WatchUserAction.REPEAT_INSTRUCTION,
            WearProtocol.decodeUserAction("""{"action": "REPEAT_INSTRUCTION", "timestamp": 1}""".toByteArray()),
        )
        assertEquals(WatchUserAction.RESUME_GUIDANCE, WearProtocol.decodeUserAction("\"RESUME_GUIDANCE\"".toByteArray()))
    }

    @Test
    fun `every action the watch encodes is decoded`() {
        // Literal output of Vibra's NavigationEventParser.encode(UserAction(type, timestamp)).
        val expected = mapOf(
            "REPEAT_INSTRUCTION" to WatchUserAction.REPEAT_INSTRUCTION,
            "PAUSE_GUIDANCE" to WatchUserAction.PAUSE_GUIDANCE,
            "RESUME_GUIDANCE" to WatchUserAction.RESUME_GUIDANCE,
            "STOP_NAVIGATION" to WatchUserAction.STOP_NAVIGATION,
            "ACKNOWLEDGE" to WatchUserAction.ACK,
        )
        expected.forEach { (name, action) ->
            val payload = """{"action":"$name","timestamp":1778344620000}""".toByteArray(Charsets.UTF_8)
            assertEquals(name, action, WearProtocol.decodeUserAction(payload))
        }
    }

    @Test
    fun `paths match the watch contract`() {
        assertEquals("/wanderai/navigation_event", WearProtocol.PATH_NAVIGATION_EVENT)
        assertEquals("/wanderai/active_route", WearProtocol.PATH_ACTIVE_ROUTE)
        assertEquals("/vibra/user_action", WearProtocol.PATH_USER_ACTION)
    }

    @Test
    fun `alert event carries the pattern the watch vibrates while paused`() {
        val event = NavigationEvent(
            action = NavigationAction.ALERT,
            instruction = "Atención",
            distanceMeters = 0,
            vibrationPattern = VibrationPattern.forAction(NavigationAction.ALERT, HapticIntensity.SOFT),
            timestampMillis = 1,
        )
        assertEquals(
            """{"action":"ALERT","instruction":"Atención","distance_meters":0,""" +
                """"vibration_pattern":{"pulses":5,"duration_ms":100,"amplitude":255},"timestamp":1}""",
            WearProtocol.encodeNavigationEvent(event).toString(Charsets.UTF_8),
        )
    }

    @Test
    fun `unknown user actions are ignored`() {
        assertNull(WearProtocol.decodeUserAction("SELF_DESTRUCT".toByteArray()))
        assertNull(WearProtocol.decodeUserAction("""{"type":"ACK"}""".toByteArray()))
        assertNull(WearProtocol.decodeUserAction(ByteArray(0)))
    }

    @Test
    fun `alerts always vibrate at full amplitude`() {
        assertEquals(255, VibrationPattern.forAction(NavigationAction.ALERT, HapticIntensity.SOFT).amplitude)
        assertEquals(90, VibrationPattern.forAction(NavigationAction.TURN_LEFT, HapticIntensity.SOFT).amplitude)
    }
}
