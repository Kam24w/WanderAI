package com.vibra.wanderai.data.supabase

import com.vibra.wanderai.domain.model.ChatConversation
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.PlaceSource
import com.vibra.wanderai.domain.repository.ChatHistoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Row of `chat_history` as read (timestamps are set by the database). */
@Serializable
internal data class ChatHistoryRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val messages: List<StoredChatMessage>,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Row as written: no timestamps, so `created_at` keeps its default and the trigger sets `updated_at`. */
@Serializable
internal data class ChatHistoryWriteRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val messages: List<StoredChatMessage>,
)

@Serializable
internal data class StoredChatMessage(
    val role: String,
    val text: String,
    val places: List<StoredPlace> = emptyList(),
)

/**
 * Snapshot of a recommended place: Google nearby places are not in the bundled
 * catalogue, so the card must be rebuilt from what was stored.
 */
@Serializable
internal data class StoredPlace(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val rating: Double,
    @SerialName("cost_cop") val costCop: Int,
    @SerialName("visit_min") val visitMin: Int,
    @SerialName("summary_es") val summaryEs: String = "",
    @SerialName("summary_en") val summaryEn: String = "",
    val source: String = PlaceSource.CATALOGUE.name,
    @SerialName("cost_estimated") val costEstimated: Boolean = false,
)

/** Domain <-> JSONB mapping of conversations (pure, unit-tested). */
internal object ChatHistoryCodec {

    fun toStored(messages: List<ChatMessage>): List<StoredChatMessage> = messages.map { message ->
        StoredChatMessage(message.role.name, message.text, message.recommendations.map { it.toStored() })
    }

    fun fromStored(messages: List<StoredChatMessage>): List<ChatMessage> = messages.mapIndexedNotNull { index, stored ->
        val role = ChatRole.entries.firstOrNull { it.name == stored.role } ?: return@mapIndexedNotNull null
        ChatMessage(index.toLong(), role, stored.text, stored.places.mapNotNull { it.toDomain() })
    }

    /** PostgREST returns timestamptz as "2026-11-08T14:03:07.123456+00:00" (UTC). */
    fun parseTimestamp(value: String?): Long {
        if (value == null || value.length < 19) return 0L
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(value.substring(0, 19))!!.time
        }.getOrDefault(0L)
    }

    private fun Place.toStored() = StoredPlace(
        id = id,
        name = name,
        category = category.name,
        lat = location.latitude,
        lng = location.longitude,
        rating = rating,
        costCop = averageCostCop,
        visitMin = visitMinutes,
        summaryEs = summary.es,
        summaryEn = summary.en,
        source = source.name,
        costEstimated = isCostEstimated,
    )

    private fun StoredPlace.toDomain(): Place? {
        val category = PlaceCategory.entries.firstOrNull { it.name == category } ?: return null
        return Place(
            id = id,
            name = name,
            category = category,
            location = GeoPoint(lat, lng),
            rating = rating,
            averageCostCop = costCop,
            visitMinutes = visitMin,
            summary = LocalizedText(es = summaryEs, en = summaryEn),
            source = PlaceSource.entries.firstOrNull { it.name == source } ?: PlaceSource.CATALOGUE,
            isCostEstimated = costEstimated,
        )
    }
}

/** `chat_history` of the signed-in user; RLS restricts every query to `auth.uid()`. */
class SupabaseChatHistoryRepository(private val client: SupabaseClient) : ChatHistoryRepository {

    override suspend fun list(): Result<List<ChatConversation>> = runSupabase("listChats") {
        val userId = client.requireUserId()
        client.from(SupabaseTables.CHAT_HISTORY)
            .select {
                filter { eq("user_id", userId) }
                order("updated_at", Order.DESCENDING)
                limit(MAX_CONVERSATIONS)
            }
            .decodeList<ChatHistoryRow>()
            .map { row ->
                ChatConversation(
                    id = row.id,
                    title = row.title,
                    messages = ChatHistoryCodec.fromStored(row.messages),
                    createdAtMillis = ChatHistoryCodec.parseTimestamp(row.createdAt),
                )
            }
    }

    override suspend fun save(conversation: ChatConversation): Result<Unit> = runSupabase("saveChat") {
        client.from(SupabaseTables.CHAT_HISTORY).upsert(
            ChatHistoryWriteRow(
                id = conversation.id,
                userId = client.requireUserId(),
                title = conversation.title,
                messages = ChatHistoryCodec.toStored(conversation.messages),
            ),
        )
        Unit
    }

    override suspend fun delete(conversationId: String): Result<Unit> = runSupabase("deleteChat") {
        val userId = client.requireUserId()
        client.from(SupabaseTables.CHAT_HISTORY).delete {
            filter {
                eq("id", conversationId)
                eq("user_id", userId)
            }
        }
        Unit
    }

    override suspend fun deleteAll(): Result<Unit> = runSupabase("deleteAllChats") {
        val userId = client.requireUserId()
        client.from(SupabaseTables.CHAT_HISTORY).delete { filter { eq("user_id", userId) } }
        Unit
    }

    private companion object {
        const val MAX_CONVERSATIONS = 100L
    }
}
