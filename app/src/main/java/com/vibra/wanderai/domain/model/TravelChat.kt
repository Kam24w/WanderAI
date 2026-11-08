package com.vibra.wanderai.domain.model

enum class ChatRole { USER, ASSISTANT }

/** One bubble of the travel chat. Assistant messages may carry recommended places. */
data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val recommendations: List<Place> = emptyList(),
)

/** A saved conversation of the travel chat (`chat_history` table). */
data class ChatConversation(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val createdAtMillis: Long,
) {
    companion object {
        const val TITLE_MAX = 60

        /** Title from the first question: "Recomiéndame un lugar tradicional para cenar…". */
        fun titleFor(firstMessage: String): String {
            val clean = firstMessage.trim().replace(Regex("\\s+"), " ")
            return if (clean.length <= TITLE_MAX) clean else clean.take(TITLE_MAX - 1).trimEnd() + "…"
        }
    }
}

/** What the AI sees besides the conversation: where the traveler is and their current filters. */
data class TravelChatContext(
    val location: TravelerLocation,
    val filters: ItineraryFilters,
    /** ISO language code of the UI ("es"/"en"); the assistant answers in it. */
    val languageCode: String,
)

/** Assistant answer: conversational [text] plus catalogue places to show as cards. */
data class ChatReply(
    val text: String,
    val places: List<Place>,
)

enum class TravelChatError { MISSING_API_KEY, UNAUTHORIZED, RATE_LIMITED, NETWORK, INVALID_RESPONSE }

class TravelChatException(val error: TravelChatError, cause: Throwable? = null) : Exception(error.name, cause)
