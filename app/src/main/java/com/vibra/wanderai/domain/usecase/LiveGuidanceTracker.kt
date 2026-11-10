package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.NavigationAction
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Live progress towards one destination, fed with GPS fixes while a route is active
 * (pure, unit-tested). It turns the traveler's position into what the Vibra watch
 * needs to keep guiding after the first cue:
 *  - [Update.RemainingDistance] for the `/wanderai/active_route` DataItem, so the
 *    watch's progress bar moves (only when it changed by [DISTANCE_STEP_METERS]);
 *  - [Update.Cue] U_TURN when the traveler keeps walking away from the destination,
 *    STRAIGHT once they are back on track, and ARRIVED inside [ARRIVAL_RADIUS_METERS].
 *
 * There is no street path here, so the straight-line distance is scaled by how much
 * longer the walking route was at the start ([initialRouteMeters]).
 */
class LiveGuidanceTracker(
    private val destination: GeoPoint,
    private val initialRouteMeters: Int,
) {
    sealed interface Update {
        data class RemainingDistance(val meters: Int) : Update
        data class Cue(val action: NavigationAction, val distanceMeters: Int) : Update
    }

    private var detourFactor: Double? = null
    private var lastReported: Int? = null
    /** Closest straight-line distance reached since the last U-turn cue. */
    private var closest = Double.MAX_VALUE
    /** Farthest distance reached since the last U-turn cue; null when not walking away. */
    private var farthestSinceUTurn: Double? = null
    var arrived: Boolean = false
        private set

    fun onLocation(point: GeoPoint): List<Update> {
        if (arrived) return emptyList()
        val straight = point.distanceTo(destination)
        val factor = detourFactor ?: initialFactor(straight).also { detourFactor = it }
        val remaining = (straight * factor).roundToInt()
        val updates = mutableListOf<Update>()

        if (straight <= ARRIVAL_RADIUS_METERS) {
            arrived = true
            lastReported = 0
            return listOf(Update.RemainingDistance(0), Update.Cue(NavigationAction.ARRIVED, 0))
        }

        val last = lastReported
        if (last == null || abs(remaining - last) >= DISTANCE_STEP_METERS) {
            lastReported = remaining
            updates += Update.RemainingDistance(remaining)
        }

        val farthest = farthestSinceUTurn
        if (farthest == null) {
            if (straight < closest) closest = straight
            if (straight - closest >= WALKING_AWAY_METERS) {
                farthestSinceUTurn = straight
                updates += Update.Cue(NavigationAction.U_TURN, 0)
            }
        } else if (straight > farthest) {
            farthestSinceUTurn = straight
        } else if (farthest - straight >= BACK_ON_TRACK_METERS) {
            // The traveler turned around: confirm the direction and start watching again.
            farthestSinceUTurn = null
            closest = straight
            updates += Update.Cue(NavigationAction.STRAIGHT, remaining)
        }
        return updates
    }

    private fun initialFactor(straight: Double): Double =
        if (straight <= 0.0 || initialRouteMeters <= 0) 1.0
        else (initialRouteMeters / straight).coerceIn(1.0, MAX_DETOUR_FACTOR)

    companion object {
        const val ARRIVAL_RADIUS_METERS = 25.0
        const val DISTANCE_STEP_METERS = 10
        /** Walking this much farther than the closest point reached means "wrong way". */
        const val WALKING_AWAY_METERS = 40.0
        const val BACK_ON_TRACK_METERS = 20.0
        private const val MAX_DETOUR_FACTOR = 2.0
    }
}
