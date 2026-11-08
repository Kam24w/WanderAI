package com.vibra.wanderai.presentation.chat

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatRole
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.TravelChatContext
import com.vibra.wanderai.domain.model.TravelChatError
import com.vibra.wanderai.domain.model.TravelChatException
import com.vibra.wanderai.domain.model.TravelerLocation
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

data class TravelChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val filters: ItineraryFilters = ItineraryFilters.DEFAULT,
    val location: TravelerLocation = ObserveTravelerLocationUseCase.FALLBACK,
    /** Error of the last failed request (message kept so it can be retried). */
    val error: TravelChatError? = null,
)

private data class Conversation(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: TravelChatError? = null,
    val failedMessage: String? = null,
)

class TravelChatViewModel(
    private val chatRepository: TravelChatRepository,
    private val planSession: PlanSession,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val conversation = MutableStateFlow(Conversation())
    private var nextId = 0L
    private var job: Job? = null

    val state: StateFlow<TravelChatUiState> =
        combine(conversation, planSession.filters, location) { c, filters, loc ->
            TravelChatUiState(c.messages, c.isSending, filters, loc, c.error)
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
        conversation.value = Conversation()
    }

    /** Card tapped: the Explore map will fly to this place. */
    fun showOnMap(placeId: String) = planSession.focusPlace(placeId)

    private fun request(history: List<ChatMessage>, message: String, languageCode: String) {
        val context = TravelChatContext(location.value, planSession.filters.value, languageCode)
        job = viewModelScope.launch {
            chatRepository.reply(history, message, context)
                .onSuccess { reply ->
                    val answer = ChatMessage(nextId++, ChatRole.ASSISTANT, reply.text, reply.places)
                    conversation.update { it.copy(messages = it.messages + answer, isSending = false) }
                }
                .onFailure { e ->
                    val error = (e as? TravelChatException)?.error ?: TravelChatError.NETWORK
                    conversation.update { it.copy(isSending = false, error = error, failedMessage = message) }
                }
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
