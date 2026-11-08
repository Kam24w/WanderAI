package com.vibra.wanderai.presentation.chat

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.ChatConversation
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.TravelChatContext
import com.vibra.wanderai.domain.model.TravelChatError
import com.vibra.wanderai.domain.model.TravelChatException
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.repository.ChatHistoryRepository
import com.vibra.wanderai.domain.repository.TravelChatRepository
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.PlanSession
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Saved conversations shown in the history sheet. */
data class ChatHistoryState(
    val conversations: List<ChatConversation> = emptyList(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    /** A delete request failed (the list was not changed). */
    val deleteFailed: Boolean = false,
)

data class TravelChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val filters: ItineraryFilters = ItineraryFilters.DEFAULT,
    val location: TravelerLocation = ObserveTravelerLocationUseCase.FALLBACK,
    /** Error of the last failed request (message kept so it can be retried). */
    val error: TravelChatError? = null,
    /** Id of the saved conversation being shown, null for a new unsaved chat. */
    val conversationId: String? = null,
    /** The last answer could not be stored in `chat_history`. */
    val saveFailed: Boolean = false,
    val history: ChatHistoryState = ChatHistoryState(),
)

private data class Conversation(
    val id: String? = null,
    val createdAtMillis: Long = 0L,
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: TravelChatError? = null,
    val failedMessage: String? = null,
    val saveFailed: Boolean = false,
)

class TravelChatViewModel(
    private val chatRepository: TravelChatRepository,
    private val historyRepository: ChatHistoryRepository,
    private val planSession: PlanSession,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
    private val clock: () -> Long = System::currentTimeMillis,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val conversation = MutableStateFlow(Conversation())
    private val history = MutableStateFlow(ChatHistoryState())
    private var nextId = 0L
    private var job: Job? = null

    val state: StateFlow<TravelChatUiState> =
        combine(conversation, planSession.filters, location, history) { c, filters, loc, h ->
            TravelChatUiState(c.messages, c.isSending, filters, loc, c.error, c.id, c.saveFailed, h)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TravelChatUiState())

    /** Sends [text] with the current location and Plan filters. [languageCode] is the UI language. */
    fun send(text: String, languageCode: String) {
        val message = text.trim()
        if (message.isEmpty() || conversation.value.isSending) return
        val history = conversation.value.messages
        val userMessage = ChatMessage(nextId++, ChatRole.USER, message)
        conversation.update { it.copy(messages = it.messages + userMessage, isSending = true, error = null, failedMessage = null) }
        request(history, message, languageCode)
    }

    /** Repeats the last message that failed (it is already in the conversation). */
    fun retry(languageCode: String) {
        val failed = conversation.value.failedMessage ?: return
        if (conversation.value.isSending) return
        val history = conversation.value.messages.dropLast(1)
        conversation.update { it.copy(isSending = true, error = null, failedMessage = null) }
        request(history, failed, languageCode)
    }

    fun newConversation() {
        job?.cancel()
        nextId = 0L
        conversation.value = Conversation()
    }

    /** Card tapped: the Explore map will fly to this place (catalogue or Google). */
    fun showOnMap(place: Place) = planSession.focusPlace(place)

    // ── History (chat_history) ────────────────────────────────────────────────

    fun loadHistory() {
        history.update { it.copy(isLoading = true, loadFailed = false, deleteFailed = false) }
        viewModelScope.launch {
            historyRepository.list()
                .onSuccess { list -> history.update { it.copy(conversations = list, isLoading = false) } }
                .onFailure { history.update { it.copy(isLoading = false, loadFailed = true) } }
        }
    }

    /** Resumes a previous conversation: new questions keep adding to it. */
    fun openConversation(saved: ChatConversation) {
        job?.cancel()
        nextId = saved.messages.size.toLong()
        conversation.value = Conversation(
            id = saved.id,
            createdAtMillis = saved.createdAtMillis,
            messages = saved.messages.mapIndexed { index, message -> message.copy(id = index.toLong()) },
        )
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            historyRepository.delete(conversationId)
                .onSuccess {
                    history.update { h -> h.copy(conversations = h.conversations.filterNot { it.id == conversationId }, deleteFailed = false) }
                    if (conversation.value.id == conversationId) newConversation()
                }
                .onFailure { history.update { it.copy(deleteFailed = true) } }
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            historyRepository.deleteAll()
                .onSuccess {
                    history.update { it.copy(conversations = emptyList(), deleteFailed = false) }
                    if (conversation.value.id != null) newConversation()
                }
                .onFailure { history.update { it.copy(deleteFailed = true) } }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private fun request(history: List<ChatMessage>, message: String, languageCode: String) {
        val context = TravelChatContext(location.value, planSession.filters.value, languageCode)
        job = viewModelScope.launch {
            chatRepository.reply(history, message, context)
                .onSuccess { reply ->
                    val answer = ChatMessage(nextId++, ChatRole.ASSISTANT, reply.text, reply.places)
                    conversation.update { it.copy(messages = it.messages + answer, isSending = false) }
                    persist()
                }
                .onFailure { e ->
                    val error = (e as? TravelChatException)?.error ?: TravelChatError.NETWORK
                    conversation.update { it.copy(isSending = false, error = error, failedMessage = message) }
                }
        }
    }

    /** Stores the conversation after every answer (insert the first time, then update). */
    private suspend fun persist() {
        val current = conversation.value
        val firstQuestion = current.messages.firstOrNull { it.role == ChatRole.USER }?.text ?: return
        val id = current.id ?: UUID.randomUUID().toString()
        val createdAt = current.id?.let { current.createdAtMillis } ?: clock()
        conversation.update { it.copy(id = id, createdAtMillis = createdAt) }
        val saved = ChatConversation(id, ChatConversation.titleFor(firstQuestion), current.messages, createdAt)
        val result = historyRepository.save(saved)
        conversation.update { if (it.id == id) it.copy(saveFailed = result.isFailure) else it }
        if (result.isSuccess) {
            history.update { h -> h.copy(conversations = listOf(saved) + h.conversations.filterNot { it.id == id }) }
        }
    }
}

@get:StringRes
val TravelChatError.messageRes: Int
    get() = when (this) {
        TravelChatError.MISSING_API_KEY -> R.string.chat_error_missing_key
        TravelChatError.UNAUTHORIZED -> R.string.chat_error_unauthorized
        TravelChatError.RATE_LIMITED -> R.string.chat_error_rate_limited
        TravelChatError.NETWORK -> R.string.chat_error_network
        TravelChatError.INVALID_RESPONSE -> R.string.chat_error_invalid
    }
