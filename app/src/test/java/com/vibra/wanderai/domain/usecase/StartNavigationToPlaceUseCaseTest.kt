package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.GuidanceState
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.TravelMode
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.model.WatchUserAction
import com.vibra.wanderai.domain.model.WearWatch
import com.vibra.wanderai.domain.repository.GuidanceAnnouncer
import com.vibra.wanderai.domain.repository.GuidanceTextProvider
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.RoutesRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartNavigationToPlaceUseCaseTest {

    private val origin = GeoPoint(1.2136, -77.2811)
    private val corner = GeoPoint(1.2145, -77.2811) // ~100 m north
    private val park = Place(
        "g:park", "Parque Infantil", PlaceCategory.PARK, GeoPoint(1.2145, -77.2802), 4.4, 0, 30, LocalizedText("", ""),
    )

    private class FakeRoutes(private val path: List<GeoPoint>?) : RoutesRepository {
        val requests = mutableListOf<List<GeoPoint>>()
        val modes = mutableListOf<TravelMode>()
        override suspend fun streetPath(waypoints: List<GeoPoint>, mode: TravelMode): Result<List<GeoPoint>> {
            requests += waypoints
            modes += mode
            return path?.let { Result.success(it) } ?: Result.failure(IllegalStateException("offline"))
        }
    }

    private class FakeItineraries : ItineraryRepository {
        override val activeItinerary = MutableStateFlow<Itinerary?>(null)
        override val savedItineraries = flowOf(emptyList<Itinerary>())
        override fun setActive(itinerary: Itinerary?) { activeItinerary.value = itinerary }
        override suspend fun save(itinerary: Itinerary) = Unit
        override suspend fun delete(itineraryId: String) = Unit
        override suspend fun refreshFromCloud() = Unit
        override suspend fun clearLocal() = Unit
    }

    private class FakeWear(connected: Boolean) : WearDataLayerRepository {
        override val connectedWatches: StateFlow<List<WearWatch>> =
            MutableStateFlow(if (connected) listOf(WearWatch("n1", "Vibra")) else emptyList())
        override val userActions = MutableSharedFlow<WatchUserAction>()
        val sent = mutableListOf<NavigationEvent>()
        val routes = mutableListOf<ActiveRouteState>()
        private val reachable = connected
        override suspend fun refreshConnectedWatches() = connectedWatches.value
        override fun onWatchMessage(path: String, data: ByteArray) = Unit
        override suspend fun sendNavigationEvent(event: NavigationEvent): Result<Int> {
            if (!reachable) return Result.failure(IllegalStateException("NOT_CONNECTED"))
            sent += event
            return Result.success(1)
        }
        override suspend fun updateActiveRoute(route: ActiveRouteState): Result<Unit> {
            routes += route
            return Result.success(Unit)
        }
    }

    /** BLE Vibra not connected: the Wear OS Data Layer is the only transport. */
    private class DisconnectedBle : VibraRepository {
        override val connectionState = MutableStateFlow<VibraConnectionState>(VibraConnectionState.Disconnected)
        override val discoveredDevices = MutableStateFlow<List<VibraDevice>>(emptyList())
        override val hapticIntensity = MutableStateFlow(HapticIntensity.STRONG)
        override val syncStatus = MutableStateFlow<WatchSyncStatus>(WatchSyncStatus.Idle)
        override val lastEvent = MutableStateFlow<HapticEvent?>(null)
        override fun isBluetoothSupported() = true
        override fun isBluetoothEnabled() = true
        override fun startScan() = Unit
        override fun stopScan() = Unit
        override fun connect(device: VibraDevice) = Unit
        override fun disconnect() = Unit
        override fun setHapticIntensity(intensity: HapticIntensity) = Unit
        override suspend fun send(command: HapticCommand) = Result.failure<Unit>(IllegalStateException("BLE off"))
        override fun updateSyncStatus(status: WatchSyncStatus) { syncStatus.value = status }
    }

    private val text = object : GuidanceTextProvider {
        override fun instruction(action: NavigationAction, distanceMeters: Int) = "$action $distanceMeters"
        override fun distance(meters: Int) = "$meters m"
    }
    private val announcer = object : GuidanceAnnouncer {
        override fun repeatInstruction(instruction: String) = Unit
        override fun guidancePaused() = Unit
        override fun guidanceResumed() = Unit
        override fun navigationStopped() = Unit
    }

    private class Setup(val useCase: StartNavigationToPlaceUseCase, val wear: FakeWear, val itineraries: FakeItineraries,
                        val ble: DisconnectedBle, val guidance: NavigationGuidanceController)

    private fun setup(path: List<GeoPoint>?, watchConnected: Boolean = true): Setup {
        val wear = FakeWear(watchConnected)
        val itineraries = FakeItineraries()
        val ble = DisconnectedBle()
        val guidance = NavigationGuidanceController(wear, itineraries.activeItinerary, ble.hapticIntensity, text, announcer, clock = { 5_000L })
        val sync = SyncItineraryToWatchUseCase(ble, guidance)
        return Setup(StartNavigationToPlaceUseCase(FakeRoutes(path), itineraries, sync, clock = { 5_000L }), wear, itineraries, ble, guidance)
    }

    @Test
    fun `sets the course along streets and sends the first turn with its vibration`() = runTest {
        val s = setup(path = listOf(origin, corner, park.location))
        s.guidance.start(backgroundScope)

        val result = s.useCase(park, origin)
        runCurrent()

        assertTrue(result.followsStreets)
        assertTrue(result.watchSynced)
        val active = s.itineraries.activeItinerary.value!!
        assertEquals(park, active.stops.single().place)
        assertEquals(result.itinerary, active)
        // ~100 m north + ~100 m east along the street path.
        assertTrue(active.totalDistanceMeters in 190..210)

        val cue = s.wear.sent.single()
        assertEquals(NavigationAction.TURN_RIGHT, cue.action)
        assertTrue(cue.distanceMeters in 95..105)
        assertEquals(3, cue.vibrationPattern.pulses)
        assertEquals(HapticIntensity.STRONG.amplitude, cue.vibrationPattern.amplitude)

        // The guidance session mirrors the destination to the watch's active_route DataItem.
        assertEquals("Parque Infantil", s.wear.routes.last().currentDestination)
        assertTrue(s.wear.routes.last().routeActive)
        assertTrue(s.ble.syncStatus.value is WatchSyncStatus.Synced)
        assertEquals(GuidanceState(lastEvent = cue), s.guidance.state.value)
    }

    @Test
    fun `falls back to a straight course when the Routes API fails`() = runTest {
        val s = setup(path = null)
        val result = s.useCase(park, origin)
        assertFalse(result.followsStreets)
        assertEquals(NavigationAction.STRAIGHT, s.wear.sent.single().action)
        assertTrue(s.itineraries.activeItinerary.value != null)
    }

    @Test
    fun `course stays active even if the watch is not reachable`() = runTest {
        val s = setup(path = listOf(origin, corner, park.location), watchConnected = false)
        val result = s.useCase(park, origin)
        assertFalse(result.watchSynced)
        assertTrue(s.itineraries.activeItinerary.value != null)
        assertTrue(s.ble.syncStatus.value is WatchSyncStatus.Failed)
    }

    @Test
    fun `first manoeuvre ignores tiny kinks and detects left turns`() {
        val west = GeoPoint(1.2145, -77.2820)
        val jitter = GeoPoint(1.21361, -77.28111) // ~1.5 m kink, must not count as a turn
        val turn = NavigationMath.firstManeuver(listOf(origin, jitter, corner, west)) as HapticCommand.Turn
        assertEquals(TurnDirection.LEFT, turn.direction)
        assertTrue(turn.distanceMeters in 95..105)

        val straight = NavigationMath.firstManeuver(listOf(origin, corner)) as HapticCommand.Straight
        assertTrue(straight.distanceMeters in 95..105)
    }
}
