package com.vibra.wanderai.data.supabase

import com.vibra.wanderai.domain.model.ChatConversation
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.PlaceSource
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistoryCodecTest {

    private val googlePark = Place(
        id = "g:A1",
        name = "Parque Infantil",
        category = PlaceCategory.PARK,
        location = GeoPoint(1.2149, -77.2788),
        rating = 4.4,
        averageCostCop = 0,
        visitMinutes = 30,
        summary = LocalizedText("Parque", "Park"),
        source = PlaceSource.GOOGLE,
        isCostEstimated = true,
    )

    @Test
    fun `messages and google place snapshots survive a jsonb round trip`() {
        val messages = listOf(
            ChatMessage(7, ChatRole.USER, "¿Un parque cerca?"),
            ChatMessage(8, ChatRole.ASSISTANT, "Prueba el Parque Infantil", listOf(googlePark)),
        )
        val json = Json.encodeToString(ChatHistoryCodec.toStored(messages))
        assertTrue(json.contains("\"role\":\"ASSISTANT\""))

        val restored = ChatHistoryCodec.fromStored(Json.decodeFromString<List<StoredChatMessage>>(json))
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT), restored.map { it.role })
        assertEquals(listOf(0L, 1L), restored.map { it.id })
        assertEquals(googlePark, restored[1].recommendations.single())
    }

    @Test
    fun `unknown roles and categories are skipped instead of crashing`() {
        val stored = listOf(
            StoredChatMessage("SYSTEM", "x"),
            StoredChatMessage("ASSISTANT", "ok", listOf(StoredPlace("p", "P", "CASINO", 1.0, 2.0, 4.0, 0, 10))),
        )
        val restored = ChatHistoryCodec.fromStored(stored)
        assertEquals(1, restored.size)
        assertTrue(restored.single().recommendations.isEmpty())
    }

    @Test
    fun `parses postgrest timestamps as utc`() {
        assertEquals(1_794_096_187_000L, ChatHistoryCodec.parseTimestamp("2026-11-08T00:03:07.123456+00:00"))
        assertEquals(0L, ChatHistoryCodec.parseTimestamp(null))
        assertEquals(0L, ChatHistoryCodec.parseTimestamp("bad"))
    }

    @Test
    fun `titles come from the first question`() {
        assertEquals("¿Dónde ceno?", ChatConversation.titleFor("  ¿Dónde   ceno?  "))
        val long = ChatConversation.titleFor("a".repeat(100))
        assertEquals(ChatConversation.TITLE_MAX, long.length)
        assertTrue(long.endsWith("…"))
    }
}
