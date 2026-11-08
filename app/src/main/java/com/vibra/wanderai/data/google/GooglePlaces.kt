package com.vibra.wanderai.data.google

import android.util.Log
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.PlaceSource
import com.vibra.wanderai.domain.repository.NearbyPlacesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Locale
import kotlin.math.roundToInt

/** Request/response of Places API (New) `places:searchNearby`, mapped to domain [Place]s. */
object GooglePlacesProtocol {
    const val ENDPOINT = "https://places.googleapis.com/v1/places:searchNearby"
    const val FIELD_MASK = "places.id,places.displayName,places.location,places.rating,places.priceLevel," +
        "places.primaryType,places.types,places.editorialSummary,places.shortFormattedAddress"
    const val MAX_RESULTS = 20
    const val MAX_RADIUS_METERS = 50_000
    const val ID_PREFIX = "g:"

    /** Two searches so food places do not crowd out parks and landmarks (20 results each). */
    val FOOD_TYPES = listOf("restaurant", "cafe", "coffee_shop", "bakery", "ice_cream_shop")
    val LEISURE_TYPES = listOf("park", "museum", "art_gallery", "tourist_attraction", "church", "observation_deck")

    private val json = Json { ignoreUnknownKeys = true }

    fun requestBody(center: GeoPoint, radiusMeters: Int, types: List<String>, languageCode: String): String =
        buildJsonObject {
            putJsonArray("includedTypes") { types.forEach { add(it) } }
            put("maxResultCount", MAX_RESULTS)
            put("rankPreference", "POPULARITY")
            put("languageCode", languageCode)
            putJsonObject("locationRestriction") {
                putJsonObject("circle") {
                    putJsonObject("center") {
                        put("latitude", center.latitude)
                        put("longitude", center.longitude)
                    }
                    put("radius", radiusMeters.coerceIn(1, MAX_RADIUS_METERS).toDouble())
                }
            }
        }.toString()

    fun parsePlaces(body: String): List<Place> {
        val places = json.parseToJsonElement(body).jsonObject["places"]?.jsonArray ?: return emptyList()
        return places.mapNotNull { (it as? JsonObject)?.toPlace() }
    }

    private fun JsonObject.toPlace(): Place? {
        val id = string("id") ?: return null
        val name = (this["displayName"] as? JsonObject)?.string("text") ?: return null
        val location = this["location"] as? JsonObject ?: return null
        val lat = (location["latitude"] as? JsonPrimitive)?.doubleOrNull ?: return null
        val lng = (location["longitude"] as? JsonPrimitive)?.doubleOrNull ?: return null
        val types = buildList {
            string("primaryType")?.let(::add)
            (this@toPlace["types"]?.jsonArray)?.forEach { t -> (t as? JsonPrimitive)?.contentOrNull?.let(::add) }
        }
        val category = categoryFor(types)
        val priceLevel = string("priceLevel")
        val description = (this["editorialSummary"] as? JsonObject)?.string("text")
            ?: string("shortFormattedAddress")
            ?: ""
        return Place(
            id = ID_PREFIX + id,
            name = name,
            category = category,
            location = GeoPoint(lat, lng),
            rating = ((this["rating"] as? JsonPrimitive)?.doubleOrNull ?: 0.0).let { (it * 10).roundToInt() / 10.0 },
            averageCostCop = estimatedCostCop(priceLevel, category),
            visitMinutes = visitMinutes(category),
            summary = LocalizedText(es = description, en = description),
            source = PlaceSource.GOOGLE,
            isCostEstimated = true,
        )
    }

    /** First matching Google type wins; the primary type comes first in [types]. */
    fun categoryFor(types: List<String>): PlaceCategory {
        for (type in types.map { it.lowercase(Locale.ROOT) }) {
            when {
                type in FOOD || type.endsWith("_restaurant") || type.endsWith("_shop") && type.contains("food") ->
                    return PlaceCategory.RESTAURANT
                type in PARKS || type.endsWith("_park") -> return PlaceCategory.PARK
                type in MUSEUMS -> return PlaceCategory.MUSEUM
                type in VIEWPOINTS -> return PlaceCategory.VIEWPOINT
                type in HERITAGE -> return PlaceCategory.HERITAGE
            }
        }
        return PlaceCategory.HERITAGE
    }

    /** Reference averages in COP for Google's price levels (Pasto, 2026). */
    fun estimatedCostCop(priceLevel: String?, category: PlaceCategory): Int = when (priceLevel) {
        "PRICE_LEVEL_FREE" -> 0
        "PRICE_LEVEL_INEXPENSIVE" -> 15_000
        "PRICE_LEVEL_MODERATE" -> 35_000
        "PRICE_LEVEL_EXPENSIVE" -> 70_000
        "PRICE_LEVEL_VERY_EXPENSIVE" -> 120_000
        else -> when (category) {
            PlaceCategory.RESTAURANT -> 25_000
            PlaceCategory.MUSEUM -> 10_000
            PlaceCategory.PARK, PlaceCategory.VIEWPOINT, PlaceCategory.HERITAGE -> 0
        }
    }

    private fun visitMinutes(category: PlaceCategory): Int = when (category) {
        PlaceCategory.RESTAURANT -> 60
        PlaceCategory.MUSEUM -> 60
        PlaceCategory.PARK -> 30
        PlaceCategory.VIEWPOINT -> 30
        PlaceCategory.HERITAGE -> 20
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private val FOOD = setOf(
        "restaurant", "cafe", "coffee_shop", "bakery", "bar", "food", "meal_takeaway", "meal_delivery",
        "ice_cream_shop", "dessert_shop", "food_court", "brunch_restaurant", "diner", "cafeteria",
        "steak_house", "pizzeria", "coffee_roastery", "tea_house", "juice_shop",
    )
    private val PARKS = setOf("park", "garden", "plaza", "playground", "botanical_garden", "dog_park", "national_park")
    private val MUSEUMS = setOf("museum", "art_gallery", "cultural_center", "history_museum", "art_museum")
    private val VIEWPOINTS = setOf("observation_deck", "scenic_spot", "hiking_area", "lookout")
    private val HERITAGE = setOf(
        "church", "place_of_worship", "historical_landmark", "monument", "tourist_attraction", "cathedral",
        "historical_place", "cultural_landmark",
    )
}

/**
 * Nearby Search with a small cache: the chat asks on every message, but results are
 * reused while the traveler stays within ~110 m for [CACHE_TTL_MS].
 */
class GooglePlacesRepository(
    private val client: GoogleApiClient,
    private val clock: () -> Long = System::currentTimeMillis,
) : NearbyPlacesRepository {

    private data class CacheKey(val lat: Double, val lng: Double, val radius: Int, val language: String)

    private val mutex = Mutex()
    private var cache: Pair<CacheKey, Pair<Long, List<Place>>>? = null

    override suspend fun nearby(center: GeoPoint, radiusMeters: Int, languageCode: String): Result<List<Place>> {
        if (!client.isConfigured) return Result.failure(IllegalStateException("MAPS_API_KEY is not configured"))
        val key = CacheKey(center.latitude.round3(), center.longitude.round3(), radiusMeters, languageCode)
        return mutex.withLock {
            cache?.let { (cachedKey, entry) ->
                if (cachedKey == key && clock() - entry.first < CACHE_TTL_MS) return@withLock Result.success(entry.second)
            }
            try {
                val places = coroutineScope {
                    listOf(GooglePlacesProtocol.FOOD_TYPES, GooglePlacesProtocol.LEISURE_TYPES).map { types ->
                        async {
                            runCatching {
                                GooglePlacesProtocol.parsePlaces(
                                    client.post(
                                        GooglePlacesProtocol.ENDPOINT,
                                        GooglePlacesProtocol.FIELD_MASK,
                                        GooglePlacesProtocol.requestBody(center, radiusMeters, types, languageCode),
                                    ),
                                )
                            }.onFailure { if (it is CancellationException) throw it }
                        }
                    }.map { it.await() }
                }
                if (places.all { it.isFailure }) throw places.first().exceptionOrNull()!!
                val merged = places.flatMap { it.getOrDefault(emptyList()) }.distinctBy { it.id }
                cache = key to (clock() to merged)
                Result.success(merged)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Nearby Search failed", e)
                Result.failure(e)
            }
        }
    }

    private fun Double.round3() = (this * 1_000).roundToInt() / 1_000.0

    private companion object {
        const val TAG = "GooglePlaces"
        const val CACHE_TTL_MS = 10 * 60 * 1_000L
    }
}
