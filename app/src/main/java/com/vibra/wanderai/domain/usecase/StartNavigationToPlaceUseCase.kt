package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.TravelMode
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.RoutesRepository
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Outcome of "Fijar rumbo". */
data class NavigationStart(
    val itinerary: Itinerary,
    /** Path along real streets (Routes API); false = straight line because the API failed. */
    val followsStreets: Boolean,
    /** The first cue and vibration reached Vibra (Wear OS Data Layer or BLE). */
    val watchSynced: Boolean,
)

/**
 * "Fijar rumbo": navigation from the traveler's position to a single [Place]
 * (typically one recommended in the AI chat).
 *  1. asks the Routes API for the walking path along real streets;
 *  2. makes a one-stop itinerary the active route, so the map draws it and
 *     [NavigationGuidanceController] mirrors `/wanderai/active_route` to the watch;
 *  3. sends the first manoeuvre of that path (turn or straight + distance) with its
 *     vibration pattern through [SyncItineraryToWatchUseCase] (Data Layer + BLE).
 */
class StartNavigationToPlaceUseCase(
    private val routesRepository: RoutesRepository,
    private val itineraryRepository: ItineraryRepository,
    private val syncItineraryToWatch: SyncItineraryToWatchUseCase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(place: Place, origin: GeoPoint): NavigationStart {
        val streets = routesRepository.streetPath(listOf(origin, place.location), TravelMode.WALK).getOrNull()
        val path = streets ?: listOf(origin, place.location)
        val meters = NavigationMath.length(path).roundToInt()
        val now = clock()
        val itinerary = Itinerary(
            id = "nav-${place.id}-$now",
            stops = listOf(
                ItineraryStop(
                    place = place,
                    travelMinutes = ceil(meters / WALKING_METERS_PER_MINUTE).toInt().coerceAtLeast(1),
                    distanceMeters = meters,
                ),
            ),
            createdAtMillis = now,
        )
        itineraryRepository.setActive(itinerary)
        val synced = syncItineraryToWatch(itinerary, origin, firstCue = NavigationMath.firstManeuver(path))
        return NavigationStart(itinerary, followsStreets = streets != null, watchSynced = synced.isSuccess)
    }

    private companion object {
        /** ~4.8 km/h, the same pace used to estimate itinerary legs. */
        const val WALKING_METERS_PER_MINUTE = 80.0
    }
}

/** Geometry helpers for turn-by-turn cues (pure, unit-tested). */
object NavigationMath {
    /** Bearing changes below this are "keep going straight". */
    const val TURN_THRESHOLD_DEGREES = 35.0

    /** Segments shorter than this (GPS noise, kerb corners) do not define a direction. */
    private const val MIN_SEGMENT_METERS = 8.0

    fun length(path: List<GeoPoint>): Double = path.zipWithNext { a, b -> a.distanceTo(b) }.sum()

    /**
     * First thing the traveler must do along [path]: the first turn sharper than
     * [TURN_THRESHOLD_DEGREES] and how far away it is, or "straight" for the whole
     * path. There is no compass heading, so the traveler is assumed to face the
     * first segment (the same assumption as itinerary cues).
     */
    fun firstManeuver(path: List<GeoPoint>): HapticCommand {
        val segments = path.zipWithNext().filter { (a, b) -> a.distanceTo(b) >= MIN_SEGMENT_METERS }
        val total = length(path).roundToInt()
        if (segments.size < 2) return HapticCommand.Straight(total)

        var walked = 0.0
        var heading = segments.first().let { (a, b) -> a.bearingTo(b) }
        for ((index, segment) in segments.withIndex()) {
            val (a, b) = segment
            val bearing = a.bearingTo(b)
            if (index > 0) {
                val delta = ((bearing - heading + 540.0) % 360.0) - 180.0
                if (abs(delta) >= TURN_THRESHOLD_DEGREES) {
                    return HapticCommand.Turn(
                        direction = if (delta > 0) TurnDirection.RIGHT else TurnDirection.LEFT,
                        distanceMeters = walked.roundToInt(),
                    )
                }
            }
            heading = bearing
            walked += a.distanceTo(b)
        }
        return HapticCommand.Straight(total)
    }
}
