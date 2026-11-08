package com.vibra.wanderai.data.ai

import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.PlaceSource
import com.vibra.wanderai.domain.model.TravelChatContext
import com.vibra.wanderai.domain.model.TravelerLocation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqProtocolTest {

    private val place = Place(
        id = "cuy-house",
        name = "Casa del Cuy",
        category = PlaceCategory.RESTAURANT,
        location = GeoPoint(1.2140, -77.2800),
        rating = 4.6,
        averageCostCop = 35_000,
        visitMinutes = 60,
        summary = LocalizedText(es = "Cocina nariñense", en = "Nariño cuisine"),
    )
    private val context = TravelChatContext(
        location = TravelerLocation(GeoPoint.PASTO_CENTER, isLive = true),
        filters = ItineraryFilters.DEFAULT.copy(budgetCop = 40_000, maxDistanceKm = 1.25f),
        languageCode = "es",
    )

    @Test
    fun `request uses the default model in json mode and sends history plus the new message last`() {
        val history = (0 until 14).map { ChatMessage(it.toLong(), if (it % 2 == 0) ChatRole.USER else ChatRole.ASSISTANT, "m$it") }
        val body = Json.parseToJsonElement(GroqProtocol.requestBody(history, "¿Dónde ceno?", context, listOf(place))).jsonObject

        assertEquals(GroqProtocol.DEFAULT_MODEL, body["model"]!!.jsonPrimitive.content)
        assertEquals("low", body["reasoning_effort"]!!.jsonPrimitive.content)
        assertEquals("json_object", body["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray
        // system + last 10 of history + new user message
        assertEquals(1 + GroqProtocol.MAX_HISTORY + 1, messages.size)
        assertEquals("system", messages.first().jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("m4", messages[1].jsonObject["content"]!!.jsonPrimitive.content)
        val last = messages.last().jsonObject
        assertEquals("user", last["role"]!!.jsonPrimitive.content)
        assertEquals("¿Dónde ceno?", last["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun `reasoning effort is only sent to gpt-oss models`() {
        val body = Json.parseToJsonElement(
            GroqProtocol.requestBody(emptyList(), "hola", context, listOf(place), model = "llama-3.3-70b-versatile"),
        ).jsonObject
        assertEquals("llama-3.3-70b-versatile", body["model"]!!.jsonPrimitive.content)
        assertNull(body["reasoning_effort"])
    }

    @Test
    fun `system prompt carries location filters and catalogue`() {
        val prompt = GroqProtocol.systemPrompt(context, listOf(place))
        assertTrue(prompt.contains("1.21360, -77.28110 (live GPS)"))
        assertTrue(prompt.contains("Budget: 40000 COP"))
        assertTrue(prompt.contains("Max walking distance: 1.25 km"))
        assertTrue(prompt.contains("cuy-house | Casa del Cuy | RESTAURANT | src=curated | cost_cop=35000"))
        assertTrue(prompt.contains("Spanish"))
    }

    @Test
    fun `google nearby places are flagged with estimated cost and sorted by distance`() {
        val nearbyPark = place.copy(
            id = "g:abc",
            name = "Parque Infantil",
            category = PlaceCategory.PARK,
            location = GeoPoint.PASTO_CENTER,
            averageCostCop = 0,
            source = PlaceSource.GOOGLE,
            isCostEstimated = true,
        )
        val prompt = GroqProtocol.systemPrompt(context, listOf(place, nearbyPark))
        assertTrue(prompt.contains("g:abc | Parque Infantil | PARK | src=google_nearby | cost_cop~0"))
        assertTrue(prompt.indexOf("g:abc") < prompt.indexOf("cuy-house")) // closer first
    }

    @Test
    fun `parses the json answer with place ids`() {
        val body = """{"choices":[{"message":{"role":"assistant","content":"{\"reply\":\"Prueba el cuy\",\"place_ids\":[\"cuy-house\",\"cuy-house\",\" \"]}"}}]}"""
        val parsed = GroqProtocol.parseResponse(body)!!
        assertEquals("Prueba el cuy", parsed.text)
        assertEquals(listOf("cuy-house"), parsed.placeIds)
    }

    @Test
    fun `accepts fenced json and plain text answers`() {
        val fenced = GroqProtocol.parseContent("```json\n{\"reply\":\"Hola\",\"place_ids\":[]}\n```")
        assertEquals("Hola", fenced.text)
        assertTrue(fenced.placeIds.isEmpty())

        val plain = GroqProtocol.parseContent("Solo texto")
        assertEquals("Solo texto", plain.text)
        assertTrue(plain.placeIds.isEmpty())
    }

    @Test
    fun `non chat bodies are rejected and error messages extracted`() {
        assertNull(GroqProtocol.parseResponse("""{"error":{"message":"Invalid API Key"}}"""))
        assertEquals("Invalid API Key", GroqProtocol.errorMessage("""{"error":{"message":"Invalid API Key"}}"""))
    }
}
