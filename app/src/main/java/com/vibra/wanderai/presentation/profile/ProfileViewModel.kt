package com.vibra.wanderai.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.model.AuthUser
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.TravelerStats
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.TravelerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: AuthUser? = null,
    val stats: TravelerStats? = null,
    val savedItineraries: List<Itinerary> = emptyList(),
    val isSavingName: Boolean = false,
    val nameUpdateFailed: Boolean = false,
)

private data class EditState(val isSaving: Boolean = false, val failed: Boolean = false)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    travelerRepository: TravelerRepository,
    private val itineraryRepository: ItineraryRepository,
) : ViewModel() {

    private val edit = MutableStateFlow(EditState())

    val state: StateFlow<ProfileUiState> = combine(
        authRepository.authState.map { (it as? AuthState.Authenticated)?.user },
        travelerRepository.observeStats(),
        itineraryRepository.savedItineraries,
        edit,
    ) { user, stats, saved, editState ->
        ProfileUiState(user, stats, saved, editState.isSaving, editState.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun openItinerary(itinerary: Itinerary) = itineraryRepository.setActive(itinerary)

    fun deleteItinerary(itinerary: Itinerary) {
        viewModelScope.launch { itineraryRepository.delete(itinerary.id) }
    }

    fun updateDisplayName(name: String, onDone: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        edit.value = EditState(isSaving = true)
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(trimmed)
            edit.update { EditState(isSaving = false, failed = result.isFailure) }
            if (result.isSuccess) onDone()
        }
    }

    fun clearNameError() = edit.update { it.copy(failed = false) }
}
