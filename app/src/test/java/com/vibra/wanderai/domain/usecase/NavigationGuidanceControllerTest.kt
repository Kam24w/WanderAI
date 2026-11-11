package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationGuidanceControllerTest {

    private class FakeWear : WearDataLayerRepository {
        override val connectedWatches = MutableStateFlow(listOf(WearWatch("n1", "Pixel Watch")))
        override val userActions = MutableSharedFlow<WatchUserAction>()
        val sent = mutableListOf<NavigationEvent>()
        val routes = mutableListOf<ActiveRouteState>()

        /** Simulates a Bluetooth gap: no nodes and every message fails. */
        var linkDown = false
            set(value) {
                field = value
                connectedWatches.value = if (value) emptyList() else listOf(WearWatch("n1", "Pixel Watch"))
            }

        override suspend fun refreshConnectedWatches(): List<WearWatch> = connectedWatches.value
        override fun onWatchMessage(path: String, data: ByteArray) = Unit

        override suspend fun sendNavigationEvent(event: NavigationEvent): Result<Int> {
            if (linkDown) return Result.failure(IllegalStateException("NOT_CONNECTED"))
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
        override fun navigationStopped() { log += "stopped" }
    }

    private val text = object : GuidanceTextProvider {
        override fun instruction(action: NavigationAction, distanceMeters: Int) = "$action $distanceMeters"
        override fun distance(meters: Int) = "$meters m"
    }

    private val wear = FakeWear()
    private val announcer = FakeAnnouncer()
    private var now = 1_000L
    private var stopRequests = 0
    private val controller = NavigationGuidanceController(
        wear = wear,
        activeItinerary = MutableStateFlow<Itinerary?>(null),
        hapticIntensity = MutableStateFlow(HapticIntensity.STRONG),
        text = text,
        announcer = announcer,
        clock = { now },
        stopRoute = { stopRequests++ },
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

    @Test
    fun `stop from the watch cancels the route on the phone and resets the session`() = runTest {
        controller.onUserAction(WatchUserAction.PAUSE_GUIDANCE)
        controller.publish(NavigationAction.TURN_LEFT, 30)

        controller.onUserAction(WatchUserAction.STOP_NAVIGATION)

        assertEquals(1, stopRequests)
        assertFalse(controller.state.value.paused)
        assertEquals(null, controller.state.value.lastEvent)
        assertEquals(listOf("paused", "stopped"), announcer.log)
    }

    @Test
    fun `an active route is tracked live until the traveler arrives`() = runTest(UnconfinedTestDispatcher()) {
        val destination = GeoPoint(1.2136, -77.2811)
        fun south(meters: Double) = GeoPoint(destination.latitude - meters / 111_195.0, destination.longitude)
        val place = Place(
            id = "plaza",
            name = "Plaza de Nariño",
            category = PlaceCategory.HERITAGE,
            location = destination,
            rating = 4.5,
            averageCostCop = 0,
            visitMinutes = 20,
            summary = LocalizedText("Plaza", "Square"),
        )
        val itinerary = MutableStateFlow<Itinerary?>(null)
        val fixes = MutableSharedFlow<GeoPoint>()
        val tracking = mutableListOf<Boolean>()
        val tracked = NavigationGuidanceController(
            wear = wear,
            activeItinerary = itinerary,
            hapticIntensity = MutableStateFlow(HapticIntensity.STRONG),
            text = text,
            announcer = announcer,
            clock = { now },
            locationUpdates = { fixes },
            onTrackingChanged = { tracking += it },
        )
        tracked.start(backgroundScope)

        itinerary.value = Itinerary("route-1", listOf(ItineraryStop(place, travelMinutes = 3, distanceMeters = 200)), 0L)
        fixes.emit(south(200.0))
        fixes.emit(south(250.0))
        fixes.emit(south(10.0))
        itinerary.value = null

        assertEquals(listOf(false, true, false), tracking)
        assertEquals(listOf(NavigationAction.U_TURN, NavigationAction.ARRIVED), wear.sent.map { it.action })
        assertTrue(wear.routes.any { it.routeActive && it.remainingDistance == "250 m" })
        assertTrue(wear.routes.any { it.routeActive && it.remainingDistance == "0 m" })
        assertFalse(wear.routes.last().routeActive)
    }

    private val plaza = Place(
        id = "plaza",
        name = "Plaza de Nariño",
        category = PlaceCategory.HERITAGE,
        location = GeoPoint(1.2136, -77.2811),
        rating = 4.5,
        averageCostCop = 0,
        visitMinutes = 20,
        summary = LocalizedText("Plaza", "Square"),
    )

    private fun southOfPlaza(meters: Double) = GeoPoint(plaza.location.latitude - meters / 111_195.0, plaza.location.longitude)

    @Test
    fun `a cue missed during a bluetooth gap is re-sent on reconnection and tracking keeps going`() =
        runTest(UnconfinedTestDispatcher()) {
            val itinerary = MutableStateFlow<Itinerary?>(null)
            val fixes = MutableSharedFlow<GeoPoint>()
            val tracked = NavigationGuidanceController(
                wear = wear,
                activeItinerary = itinerary,
                hapticIntensity = MutableStateFlow(HapticIntensity.STRONG),
                text = text,
                announcer = announcer,
                clock = { now },
                locationUpdates = { fixes },
            )
            tracked.start(backgroundScope)
            itinerary.value = Itinerary("route-1", listOf(ItineraryStop(plaza, travelMinutes = 3, distanceMeters = 200)), 0L)
            fixes.emit(southOfPlaza(200.0))

            wear.linkDown = true
            fixes.emit(southOfPlaza(250.0)) // walking away: U_TURN, but the watch is unreachable
            assertTrue(wear.sent.isEmpty())
            assertTrue("remaining distance is still written as a DataItem", wear.routes.any { it.remainingDistance == "250 m" })

            now += 20_000
            wear.linkDown = false // Bluetooth back
            assertEquals(listOf(NavigationAction.U_TURN), wear.sent.map { it.action })
            assertEquals("re-sent with a fresh timestamp", now, wear.sent.single().timestampMillis)

            fixes.emit(southOfPlaza(150.0))
            assertTrue(wear.routes.any { it.routeActive && it.remainingDistance == "150 m" })
            assertEquals("no duplicate re-send", 1, wear.sent.count { it.action == NavigationAction.U_TURN })
        }

    @Test
    fun `stale missed cues are dropped instead of vibrating late`() = runTest {
        wear.linkDown = true
        controller.publish(NavigationAction.TURN_LEFT, 30)
        wear.linkDown = false
        now += 61_000

        controller.retryUndeliveredCue()

        assertTrue(wear.sent.isEmpty())
    }

    @Test
    fun `a failing location source stops tracking without crashing the session`() = runTest(UnconfinedTestDispatcher()) {
        val itinerary = MutableStateFlow<Itinerary?>(null)
        val errors = mutableListOf<Throwable>()
        val tracked = NavigationGuidanceController(
            wear = wear,
            activeItinerary = itinerary,
            hapticIntensity = MutableStateFlow(HapticIntensity.STRONG),
            text = text,
            announcer = announcer,
            clock = { now },
            locationUpdates = { flow { throw SecurityException("location permission revoked") } },
            onTrackingError = { errors += it },
        )
        tracked.start(backgroundScope)
        itinerary.value = Itinerary("route-1", listOf(ItineraryStop(plaza, travelMinutes = 3, distanceMeters = 200)), 0L)

        assertEquals(1, errors.size)
        assertTrue("the route stays mirrored to the watch", wear.routes.last().routeActive)
        tracked.publish(NavigationAction.TURN_RIGHT, 20)
        assertEquals(NavigationAction.TURN_RIGHT, wear.sent.last().action)
    }
}
