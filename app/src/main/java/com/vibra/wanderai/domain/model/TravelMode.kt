package com.vibra.wanderai.domain.model

import kotlin.math.ceil

/**
 * How the traveler moves on "Fijar rumbo". It decides which streets the route may
 * use (Routes API travel mode), the estimated travel time and how tolerant the live
 * guidance is (a car cannot turn around in 40 m like someone on foot).
 *
 * The watch protocol does not change: Vibra still receives left / right / straight /
 * U-turn / arrival, whatever the mode.
 */
enum class TravelMode(
    /** Average urban speed used for the time estimate. */
    val metersPerMinute: Double,
    /** Inside this radius the traveler has arrived (vehicles need room to park). */
    val arrivalRadiusMeters: Double,
    /** Minimum change of the remaining distance before the watch is updated. */
    val distanceStepMeters: Int,
    /** Moving this much farther than the closest point reached means "wrong way". */
    val wrongWayMeters: Double,
    /** Coming back this much after a U-turn cue confirms the traveler turned around. */
    val backOnTrackMeters: Double,
) {
    /** ~4.8 km/h. */
    WALK(80.0, arrivalRadiusMeters = 25.0, distanceStepMeters = 10, wrongWayMeters = 40.0, backOnTrackMeters = 20.0),

    /** ~25 km/h in city traffic. */
    MOTORCYCLE(420.0, arrivalRadiusMeters = 40.0, distanceStepMeters = 25, wrongWayMeters = 120.0, backOnTrackMeters = 50.0),

    /** ~21 km/h in city traffic (one-way streets, lights, parking). */
    CAR(350.0, arrivalRadiusMeters = 50.0, distanceStepMeters = 25, wrongWayMeters = 150.0, backOnTrackMeters = 60.0);

    /** Whole minutes to cover [meters] (at least 1). */
    fun minutesFor(meters: Int): Int = ceil(meters / metersPerMinute).toInt().coerceAtLeast(1)

    companion object {
        val DEFAULT = WALK

        /** Stored name back to a mode; unknown or missing values fall back to walking. */
        fun fromName(name: String?): TravelMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
