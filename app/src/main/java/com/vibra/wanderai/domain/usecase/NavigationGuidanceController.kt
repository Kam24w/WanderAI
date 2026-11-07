package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ActiveRouteState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Application-scoped coordinator of the Wear OS guidance session:
 *  - turns cues into [NavigationEvent]s for `/wanderai/navigation_event`,
 *  - mirrors the active itinerary to the `/wanderai/active_route` DataItem,
 *  - reacts to ACK / REPEAT / PAUSE / RESUME sent from the watch.
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
) {
    private val _state = MutableStateFlow(GuidanceState())
    val state: StateFlow<GuidanceState> = _state.asStateFlow()

    private var job: Job? = null

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
        return wear.sendNavigationEvent(event)
    }

    /** A route sent explicitly from the phone starts a fresh session, so it lifts any pause. */
    suspend fun startRoute(firstCue: HapticCommand): Result<Int> {
        _state.update { it.copy(paused = false) }
        return publish(firstCue)
    }

    suspend fun onUserAction(action: WatchUserAction) {
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
        HapticCommand.Ping -> NavigationAction.ALERT
        // The watch receives the route through the active_route DataItem instead.
        is HapticCommand.ItinerarySync -> null
    }

    private fun HapticCommand.distanceMeters(): Int = when (this) {
        is HapticCommand.Turn -> distanceMeters
        is HapticCommand.Straight -> distanceMeters
        else -> 0
    }
}
