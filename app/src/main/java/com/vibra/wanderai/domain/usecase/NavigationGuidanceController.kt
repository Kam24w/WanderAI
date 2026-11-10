package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.GuidanceState
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.model.VibrationPattern
import com.vibra.wanderai.domain.model.WatchUserAction
import com.vibra.wanderai.domain.repository.GuidanceAnnouncer
import com.vibra.wanderai.domain.repository.GuidanceTextProvider
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Application-scoped coordinator of the Wear OS guidance session:
 *  - turns cues into [NavigationEvent]s for `/wanderai/navigation_event`,
 *  - mirrors the active itinerary to the `/wanderai/active_route` DataItem,
 *  - reacts to ACK / REPEAT / PAUSE / RESUME / STOP sent from the watch,
 *  - tracks the traveler during a route ([LiveGuidanceTracker]): live remaining
 *    distance plus U_TURN / STRAIGHT / ARRIVED cues.
 *
 * While paused, cues are remembered but not sent (except [NavigationAction.ALERT]);
 * the latest one is re-sent on resume.
 */
class NavigationGuidanceController(
    private val wear: WearDataLayerRepository,
    private val activeItinerary: Flow<Itinerary?>,
    private val hapticIntensity: StateFlow<HapticIntensity>,
    private val text: GuidanceTextProvider,
    private val announcer: GuidanceAnnouncer,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Cancels the active route on the phone (clears the itinerary and the map). */
    private val stopRoute: () -> Unit = {},
    /** GPS fixes while collected; drives [LiveGuidanceTracker] during a route. */
    private val locationUpdates: () -> Flow<GeoPoint> = { emptyFlow() },
    /** True while a route is being tracked (keeps location alive with the screen off). */
    private val onTrackingChanged: (Boolean) -> Unit = {},
    /** Live tracking failed (e.g. location permission revoked); the route itself stays. */
    private val onTrackingError: (Throwable) -> Unit = {},
) {
    private val _state = MutableStateFlow(GuidanceState())
    val state: StateFlow<GuidanceState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Last cue the watch did not receive (link down: Bluetooth gap, tunnel restart...).
     * It is re-sent with a fresh timestamp as soon as the link is back, if it is
     * still recent enough to be useful ([CUE_RETRY_WINDOW_MS]).
     */
    @Volatile private var undeliveredCue: NavigationEvent? = null

    /** Starts listening to the watch and syncing the route. Safe to call more than once. */
    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            launch { wear.userActions.collect { onUserAction(it) } }
            launch {
                activeItinerary
                    .map(::routeStateFor)
                    .distinctUntilChanged()
                    .collect { route ->
                        if (!route.routeActive) _state.value = GuidanceState()
                        wear.updateActiveRoute(route)
                    }
            }
            launch {
                activeItinerary
                    .map { itinerary -> itinerary?.stops?.firstOrNull()?.let { itinerary.id to it } }
                    .distinctUntilChanged()
                    .collectLatest { target ->
                        onTrackingChanged(target != null)
                        target?.let { (_, stop) -> track(stop.place.name, stop.place.location, stop.distanceMeters) }
                    }
            }
        }
    }

    /**
     * Follows the traveler towards the first stop: keeps the watch's remaining
     * distance up to date and sends U_TURN / STRAIGHT / ARRIVED as they walk.
     */
    private suspend fun track(name: String, destination: GeoPoint, routeMeters: Int) = coroutineScope {
        // Watching the watch list during a route also polls the Data Layer nodes, so a
        // reconnection is noticed even when the traveler stands still (no GPS fixes).
        launch {
            wear.connectedWatches
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { linked -> if (linked) retryUndeliveredCue() }
        }
        val tracker = LiveGuidanceTracker(destination, routeMeters)
        try {
            locationUpdates().collect { point ->
                retryUndeliveredCue()
                tracker.onLocation(point).forEach { update ->
                    when (update) {
                        // A DataItem: Play services keeps it and syncs it when the watch reconnects.
                        is LiveGuidanceTracker.Update.RemainingDistance -> wear.updateActiveRoute(
                            ActiveRouteState(name, text.distance(update.meters), routeActive = true),
                        )
                        is LiveGuidanceTracker.Update.Cue -> publish(update.action, update.distanceMeters)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // GPS provider gone or permission revoked mid-route: stop tracking, keep the app alive.
            onTrackingError(e)
        }
    }

    /** Publishes a BLE-style [command]; commands with no watch equivalent are skipped (success, 0 watches). */
    suspend fun publish(command: HapticCommand): Result<Int> {
        val action = command.toNavigationAction() ?: return Result.success(0)
        return publish(action, command.distanceMeters())
    }

    /** Builds and sends a cue. Succeeds with 0 when held because guidance is paused. */
    suspend fun publish(action: NavigationAction, distanceMeters: Int): Result<Int> {
        val event = buildEvent(action, distanceMeters)
        val paused = _state.updateAndGet { it.copy(lastEvent = event) }.paused
        if (paused && action != NavigationAction.ALERT) return Result.success(0)
        return wear.sendNavigationEvent(event).also { result ->
            undeliveredCue = if (result.isSuccess) null else event
        }
    }

    /** Re-sends [undeliveredCue] if there is one and it is still fresh; never throws. */
    suspend fun retryUndeliveredCue() {
        val cue = undeliveredCue ?: return
        if (clock() - cue.timestampMillis > CUE_RETRY_WINDOW_MS || _state.value.paused) {
            undeliveredCue = null
            return
        }
        if (wear.sendNavigationEvent(cue.copy(timestampMillis = clock())).isSuccess) undeliveredCue = null
    }

    /** A route sent explicitly from the phone starts a fresh session, so it lifts any pause. */
    suspend fun startRoute(firstCue: HapticCommand): Result<Int> {
        _state.update { it.copy(paused = false) }
        return publish(firstCue)
    }

    suspend fun onUserAction(action: WatchUserAction) {
        // A message from the watch proves the link is back.
        if (action != WatchUserAction.PAUSE_GUIDANCE) retryUndeliveredCue()
        when (action) {
            WatchUserAction.ACK -> _state.update { it.copy(lastAckMillis = clock()) }

            WatchUserAction.REPEAT_INSTRUCTION ->
                _state.value.lastEvent?.let { announcer.repeatInstruction(it.instruction) }

            WatchUserAction.PAUSE_GUIDANCE -> {
                if (_state.value.paused) return
                _state.update { it.copy(paused = true) }
                announcer.guidancePaused()
            }

            WatchUserAction.RESUME_GUIDANCE -> {
                if (!_state.value.paused) return
                val resumed = _state.updateAndGet { it.copy(paused = false) }
                announcer.guidanceResumed()
                resumed.lastEvent?.let { wear.sendNavigationEvent(it.copy(timestampMillis = clock())) }
            }

            // Clearing the itinerary also writes an inactive route DataItem for the watch.
            WatchUserAction.STOP_NAVIGATION -> {
                _state.value = GuidanceState()
                stopRoute()
                announcer.navigationStopped()
            }
        }
    }

    private fun buildEvent(action: NavigationAction, distanceMeters: Int): NavigationEvent {
        val meters = distanceMeters.coerceAtLeast(0)
        return NavigationEvent(
            action = action,
            instruction = text.instruction(action, meters),
            distanceMeters = meters,
            vibrationPattern = VibrationPattern.forAction(action, hapticIntensity.value),
            timestampMillis = clock(),
        )
    }

    /** No progress tracking yet: the watch is guiding towards the first stop. */
    private fun routeStateFor(itinerary: Itinerary?): ActiveRouteState {
        val first = itinerary?.stops?.firstOrNull() ?: return ActiveRouteState.INACTIVE
        return ActiveRouteState(
            currentDestination = first.place.name,
            remainingDistance = text.distance(itinerary.totalDistanceMeters),
            routeActive = true,
        )
    }

    private fun HapticCommand.toNavigationAction(): NavigationAction? = when (this) {
        is HapticCommand.Turn ->
            if (direction == TurnDirection.LEFT) NavigationAction.TURN_LEFT else NavigationAction.TURN_RIGHT
        is HapticCommand.Straight -> NavigationAction.STRAIGHT
        HapticCommand.Arrived -> NavigationAction.ARRIVED
        HapticCommand.UTurn -> NavigationAction.U_TURN
        HapticCommand.Ping -> NavigationAction.ALERT
        // The watch receives the route through the active_route DataItem instead.
        is HapticCommand.ItinerarySync -> null
    }

    private fun HapticCommand.distanceMeters(): Int = when (this) {
        is HapticCommand.Turn -> distanceMeters
        is HapticCommand.Straight -> distanceMeters
        else -> 0
    }

    private companion object {
        /** A missed cue older than this is no longer useful to the traveler. */
        const val CUE_RETRY_WINDOW_MS = 60_000L
    }
}
