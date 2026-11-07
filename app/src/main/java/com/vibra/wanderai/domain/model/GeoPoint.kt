package com.vibra.wanderai.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** WGS-84 coordinate. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    /** Great-circle distance in metres (haversine). */
    fun distanceTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Initial bearing towards [other], in degrees 0..360 (0 = north). */
    fun bearingTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        /** Plaza de Nariño, San Juan de Pasto. */
        val PASTO_CENTER = GeoPoint(latitude = 1.2136, longitude = -77.2811)
    }
}

/** Where the traveler is. [isLive] is false when we fall back to Pasto's centre. */
data class TravelerLocation(
    val point: GeoPoint,
    val isLive: Boolean,
)
