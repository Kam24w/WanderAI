package com.vibra.wanderai.data.ai

import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceSource
import com.vibra.wanderai.domain.model.TravelChatContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Wire format of the Groq chat completions API (OpenAI compatible) for the travel chat.
 *
 * The model only recommends places from the bundled catalogue and answers with a JSON
 * object `{"reply": "...", "place_ids": ["..."]}`, so every suggestion can be shown as a
 * card and located on the map.
 */
object GroqProtocol {
    const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

    /**
     * Default model (override with GROQ_MODEL in local.properties). `llama-3.3-70b-versatile`
     * is no longer offered to this project's Groq key (`model_not_found`, Nov 2026).
     */
    const val DEFAULT_MODEL = "openai/gpt-oss-120b"

    /** Previous messages sent with each request (keeps the prompt small). */
    const val MAX_HISTORY = 10
    const val MAX_RECOMMENDATIONS = 4

    private val json = Json { ignoreUnknownKeys = true }

    data class ParsedReply(val text: String, val placeIds: List<String>)

    fun requestBody(
        history: List<ChatMessage>,
        message: String,
        context: TravelChatContext,
        catalogue: List<Place>,
        model: String = DEFAULT_MODEL,
    ): String = buildJsonObject {
        put("model", model)
        put("temperature", 0.6)
        put("max_tokens", 900)
        // gpt-oss models reason before answering; "low" keeps the chat fast.
        if (model.startsWith("openai/gpt-oss")) put("reasoning_effort", "low")
        putJsonObject("response_format") { put("type", "json_object") }
        putJsonArray("messages") {
            addJsonObject {
                put("role", "system")
                put("content", systemPrompt(context, catalogue))
            }
            history.takeLast(MAX_HISTORY).forEach { previous ->
                addJsonObject {
                    put("role", if (previous.role == ChatRole.USER) "user" else "assistant")
                    put("content", previous.text)
                }
            }
            addJsonObject {
                put("role", "user")
                put("content", message)
            }
        }
    }.toString()

    fun systemPrompt(context: TravelChatContext, catalogue: List<Place>): String {
        val filters = context.filters
        val origin = context.location.point
        val language = if (context.languageCode.startsWith("en")) "English" else "Spanish (Colombia)"
        val categories = filters.categories.joinToString { it.name }.ifEmpty { "any" }
        val places = catalogue.sortedBy { origin.distanceTo(it.location) }.joinToString("\n") { place ->
            val meters = origin.distanceTo(place.location).roundToInt()
            listOf(
                place.id,
                place.name,
                place.category.name,
                if (place.source == PlaceSource.GOOGLE) "src=google_nearby" else "src=curated",
                (if (place.isCostEstimated) "cost_cop~" else "cost_cop=") + place.averageCostCop,
                "rating=${place.rating}",
                "distance_m=$meters",
                "visit_min=${place.visitMinutes}",
                place.summary.resolve(context.languageCode),
            ).joinToString(" | ")
        }
        return """
            You are WanderAI, a warm local travel guide for San Juan de Pasto, Colombia.
            Answer ONLY in $language, conversationally, in at most 120 words. Mention prices in COP
            and walking distances when useful. Never invent places: recommend only from CATALOGUE.

            TRAVELER
            - Location: ${String.format(Locale.US, "%.5f, %.5f", origin.latitude, origin.longitude)}${if (context.location.isLive) " (live GPS)" else " (city centre, no GPS)"}
            - Available time: ${filters.availableMinutes} min
            - Budget: ${filters.budgetCop} COP in total
            - Max walking distance: ${String.format(Locale.US, "%.2f", filters.maxDistanceKm)} km
            - Preferred categories: $categories
            Respect these filters unless the traveler's message asks otherwise (explicit limits in the
            message, such as "less than 40k", take priority).

            CATALOGUE (id | name | category | source | cost | rating | distance | visit | summary)
            "curated" = landmarks of Pasto; "google_nearby" = real places around the traveler right now
            (Google Places). Prefer google_nearby places when the traveler asks for something close by,
            such as a park, café or restaurant. "cost_cop~" is an estimate from Google's price level: say
            "aprox."/"approx." when you mention it.
            $places

            Respond with a JSON object and nothing else:
            {"reply": "<your answer>", "place_ids": ["<catalogue id>", ...]}
            place_ids: 0 to $MAX_RECOMMENDATIONS ids from CATALOGUE, most relevant first; empty when you are
            not recommending places. Do not write ids inside "reply".
        """.trimIndent()
    }

    /** Extracts the assistant answer from a Groq response body. Null if it is not a chat completion. */
    fun parseResponse(body: String): ParsedReply? {
        val content = runCatching {
            json.parseToJsonElement(body).jsonObject["choices"]!!.jsonArray[0]
                .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
        }.getOrNull() ?: return null
        return parseContent(content)
    }

    /** The model is asked for JSON; plain text (or fenced JSON) is still accepted. */
    fun parseContent(content: String): ParsedReply {
        val trimmed = content.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = runCatching { json.parseToJsonElement(trimmed) as? JsonObject }.getOrNull()
            ?: return ParsedReply(content.trim(), emptyList())
        val reply = (obj["reply"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        val ids = (obj["place_ids"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()
        return ParsedReply(reply.ifEmpty { trimmed }, ids)
    }

    /** Groq error bodies look like `{"error":{"message":"..."}}`. */
    fun errorMessage(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
    }.getOrNull()
}
