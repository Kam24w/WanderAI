package com.vibra.wanderai.data.ai

import android.util.Log
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatReply
import com.vibra.wanderai.domain.model.TravelChatContext
import com.vibra.wanderai.domain.model.TravelChatError
import com.vibra.wanderai.domain.model.TravelChatException
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.repository.NearbyPlacesRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import com.vibra.wanderai.domain.repository.TravelChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import kotlin.math.roundToInt

/**
 * Travel chat backed by Groq ([model], see [GroqProtocol.DEFAULT_MODEL]). Each request carries the
 * traveler's coordinates, the Plan filters, the curated catalogue and the real places around the
 * traveler from Google Places Nearby Search ([nearbyPlaces]). The answer's place ids are resolved
 * against that same list, so the UI gets real [Place]s with coordinates. If Nearby Search fails
 * the chat keeps working with the curated catalogue only.
 */
class GroqRecommendationRepository(
    private val apiKey: String,
    private val placesRepository: PlacesRepository,
    private val nearbyPlaces: NearbyPlacesRepository? = null,
    private val model: String = GroqProtocol.DEFAULT_MODEL,
) : TravelChatRepository {

    override suspend fun reply(
        history: List<ChatMessage>,
        message: String,
        context: TravelChatContext,
    ): Result<ChatReply> {
        if (apiKey.isBlank()) return Result.failure(TravelChatException(TravelChatError.MISSING_API_KEY))
        return try {
            val catalogue = catalogueFor(context)
            val body = GroqProtocol.requestBody(history, message, context, catalogue, model.ifBlank { GroqProtocol.DEFAULT_MODEL })
            val parsed = GroqProtocol.parseResponse(post(body))
                ?: throw TravelChatException(TravelChatError.INVALID_RESPONSE)
            val byId = catalogue.associateBy { it.id }
            val places = parsed.placeIds.mapNotNull(byId::get).take(GroqProtocol.MAX_RECOMMENDATIONS)
            Result.success(ChatReply(parsed.text, places))
        } catch (e: CancellationException) {
            throw e
        } catch (e: TravelChatException) {
            Log.w(TAG, "Groq chat failed: ${e.error}", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.w(TAG, "Groq chat network error", e)
            Result.failure(TravelChatException(TravelChatError.NETWORK, e))
        }
    }

    /** Curated places + Google nearby places (radius = Plan max distance), without duplicate names. */
    private suspend fun catalogueFor(context: TravelChatContext): List<Place> {
        val curated = placesRepository.getPlaces()
        val radius = (context.filters.maxDistanceKm * 1_000).roundToInt().coerceAtLeast(MIN_NEARBY_RADIUS_M)
        val nearby = nearbyPlaces?.nearby(context.location.point, radius, context.languageCode)
            ?.onFailure { Log.w(TAG, "Nearby places unavailable, using the curated catalogue only", it) }
            ?.getOrNull()
            .orEmpty()
        val curatedNames = curated.map { it.name.normalizedName() }.toSet()
        return curated + nearby.filter { it.name.normalizedName() !in curatedNames }
    }

    private suspend fun post(body: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(GroqProtocol.ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> text
                else -> {
                    Log.w(TAG, "Groq HTTP $code: ${GroqProtocol.errorMessage(text) ?: text.take(200)}")
                    throw TravelChatException(
                        when (code) {
                            401, 403 -> TravelChatError.UNAUTHORIZED
                            429 -> TravelChatError.RATE_LIMITED
                            else -> TravelChatError.NETWORK
                        },
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "GroqChat"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 45_000
        const val MIN_NEARBY_RADIUS_M = 500
    }
}

/** "Plaza de Nariño" and "plaza de narino" are the same place for de-duplication. */
internal fun String.normalizedName(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .filter(Char::isLetterOrDigit)
