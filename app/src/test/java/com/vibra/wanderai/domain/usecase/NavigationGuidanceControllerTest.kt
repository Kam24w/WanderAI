package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.model.WatchUserAction
import com.vibra.wanderai.domain.model.WearWatch
import com.vibra.wanderai.domain.repository.GuidanceAnnouncer
import com.vibra.wanderai.domain.repository.GuidanceTextProvider
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationGuidanceControllerTest {

    private class FakeWear : WearDataLayerRepository {
        override val connectedWatches: StateFlow<List<WearWatch>> = MutableStateFlow(listOf(WearWatch("n1", "Pixel Watch")))
        override val userActions = MutableSharedFlow<WatchUserAction>()
        val sent = mutableListOf<NavigationEvent>()
        val routes = mutableListOf<ActiveRouteState>()

        override suspend fun refreshConnectedWatches(): List<WearWatch> = connectedWatches.value

        override suspend fun sendNavigationEvent(event: NavigationEvent): Result<Int> {
            sent += event
            return Result.success(1)
        }

        override suspend fun updateActiveRoute(route: ActiveRouteState): Result<Unit> {
            routes += route
            return Result.success(Unit)
        }
    }

    private class FakeAnnouncer : GuidanceAnnouncer {
        val log = mutableListOf<String>()
        override fun repeatInstruction(instruction: String) { log += "repeat:$instruction" }
        override fun guidancePaused() { log += "paused" }
        override fun guidanceResumed() { log += "resumed" }
    }

    private val text = object : GuidanceTextProvider {
        override fun instruction(action: NavigationAction, distanceMeters: Int) = "$action $distanceMeters"
        override fun distance(meters: Int) = "$meters m"
    }

    private val wear = FakeWear()
    private val announcer = FakeAnnouncer()
    private var now = 1_000L
    private val controller = NavigationGuidanceController(
        wear = wear,
        activeItinerary = MutableStateFlow<Itinerary?>(null),
        hapticIntensity = MutableStateFlow(HapticIntensity.STRONG),
        text = text,
        announcer = announcer,
        clock = { now },
    )

    @Test
    fun `haptic commands become navigation events with pattern and text`() = runTest {
        controller.publish(HapticCommand.Turn(TurnDirection.RIGHT, 80))

        val event = wear.sent.single()
        assertEquals(NavigationAction.TURN_RIGHT, event.action)
        assertEquals("TURN_RIGHT 80", event.instruction)
        assertEquals(80, event.distanceMeters)
        assertEquals(3, event.vibrationPattern.pulses)
        assertEquals(255, event.vibrationPattern.amplitude)
        assertEquals(1_000L, event.timestampMillis)
    }

    @Test
    fun `itinerary sync header is not sent as a navigation event`() = runTest {
        controller.publish(HapticCommand.ItinerarySync(stopCount = 3, firstStopMeters = 100))
        assertTrue(wear.sent.isEmpty())
    }

    @Test
    fun `pause holds cues and resume re-sends the latest one`() = runTest {
        controller.onUserAction(WatchUserAction.PAUSE_GUIDANCE)
        controller.publish(NavigationAction.STRAIGHT, 40)
        controller.publish(NavigationAction.U_TURN, 50)
        assertTrue(wear.sent.isEmpty())
        assertTrue(controller.state.value.paused)

        now = 5_000L
        controller.onUserAction(WatchUserAction.RESUME_GUIDANCE)

        val resent = wear.sent.single()
        assertEquals(NavigationAction.U_TURN, resent.action)
        assertEquals(5_000L, resent.timestampMillis)
        assertFalse(controller.state.value.paused)
        assertEquals(listOf("paused", "resumed"), announcer.log)
    }

    @Test
    fun `alerts go through while paused`() = runTest {
        controller.onUserAction(WatchUserAction.PAUSE_GUIDANCE)
        controller.publish(NavigationAction.ALERT, 0)
        assertEquals(NavigationAction.ALERT, wear.sent.single().action)
    }

    @Test
    fun `repeat announces the last instruction on the phone only`() = runTest {
        controller.onUserAction(WatchUserAction.REPEAT_INSTRUCTION) // nothing to repeat yet
        controller.publish(NavigationAction.TURN_LEFT, 30)
        controller.onUserAction(WatchUserAction.REPEAT_INSTRUCTION)

        assertEquals(listOf("repeat:TURN_LEFT 30"), announcer.log)
        assertEquals(1, wear.sent.size)
    }

    @Test
    fun `ack records the confirmation time`() = runTest {
        now = 42L
        controller.onUserAction(WatchUserAction.ACK)
        assertEquals(42L, controller.state.value.lastAckMillis)
    }

    @Test
    fun `starting a route from the phone lifts the pause`() = runTest {
        controller.onUserAction(WatchUserAction.PAUSE_GUIDANCE)
        controller.startRoute(HapticCommand.Straight(120))
        assertFalse(controller.state.value.paused)
        assertEquals(NavigationAction.STRAIGHT, wear.sent.single().action)
    }
}
