package com.vibra.wanderai.data.google

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.repository.RoutesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Google encoded polyline format (precision 1e-5), as returned by the Routes API. */
object PolylineCodec {

    fun decode(encoded: String): List<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            val (dLat, afterLat) = nextValue(encoded, index)
            val (dLng, afterLng) = nextValue(encoded, afterLat)
            index = afterLng
            lat += dLat
            lng += dLng
            points += GeoPoint(lat / 1e5, lng / 1e5)
        }
        return points
    }

    private fun nextValue(encoded: String, start: Int): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var index = start
        var byte: Int
        do {
            require(index < encoded.length) { "Truncated polyline" }
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)
        val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        return value to index
    }
}

/** Request/response of `directions/v2:computeRoutes` for walking paths. */
object GoogleRoutesProtocol {
    const val ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes"
    const val FIELD_MASK = "routes.polyline.encodedPolyline"

    /** The Routes API accepts at most 25 intermediate waypoints. */
    const val MAX_INTERMEDIATES = 25

    private val json = Json { ignoreUnknownKeys = true }

    fun requestBody(waypoints: List<GeoPoint>): String {
        require(waypoints.size >= 2) { "A route needs an origin and a destination" }
        return buildJsonObject {
            putWaypoint("origin", waypoints.first())
            putWaypoint("destination", waypoints.last())
            val intermediates = waypoints.subList(1, waypoints.lastIndex).take(MAX_INTERMEDIATES)
            if (intermediates.isNotEmpty()) {
                putJsonArray("intermediates") {
                    intermediates.forEach { point -> addJsonObject { putLocation(point) } }
                }
            }
            put("travelMode", "WALK")
            put("polylineEncoding", "ENCODED_POLYLINE")
        }.toString()
    }

    /** Decoded path of the first route; empty when Google found no walking route. */
    fun parsePath(body: String): List<GeoPoint> {
        val routes = json.parseToJsonElement(body).jsonObject["routes"]?.jsonArray ?: return emptyList()
        val encoded = routes.firstOrNull()?.jsonObject?.get("polyline")?.jsonObject
            ?.get("encodedPolyline")?.jsonPrimitive?.content ?: return emptyList()
        return PolylineCodec.decode(encoded)
    }

    private fun JsonObjectBuilder.putWaypoint(name: String, point: GeoPoint) = putJsonObject(name) { putLocation(point) }

    private fun JsonObjectBuilder.putLocation(point: GeoPoint) {
        putJsonObject("location") {
            putJsonObject("latLng") {
                put("latitude", point.latitude)
                put("longitude", point.longitude)
            }
        }
    }
}

/** Walking paths along real streets from the Google Routes API. */
class GoogleRoutesRepository(private val client: GoogleApiClient) : RoutesRepository {

    override suspend fun walkingPath(waypoints: List<GeoPoint>): Result<List<GeoPoint>> = try {
        check(client.isConfigured) { "MAPS_API_KEY is not configured" }
        val path = GoogleRoutesProtocol.parsePath(
            client.post(GoogleRoutesProtocol.ENDPOINT, GoogleRoutesProtocol.FIELD_MASK, GoogleRoutesProtocol.requestBody(waypoints)),
        )
        if (path.size < 2) Result.failure(IllegalStateException("No walking route")) else Result.success(path)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
