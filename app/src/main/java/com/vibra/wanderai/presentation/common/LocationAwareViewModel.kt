package com.vibra.wanderai.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Base for screens that follow the traveler. Call [refreshLocation] after the
 * location permission is granted to restart the GPS subscription.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class LocationAwareViewModel(
    observeTravelerLocation: ObserveTravelerLocationUseCase,
) : ViewModel() {

    private val subscription = MutableStateFlow(0)

    protected val location: StateFlow<TravelerLocation> =
        subscription
            .flatMapLatest { observeTravelerLocation() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObserveTravelerLocationUseCase.FALLBACK)

    fun refreshLocation() = subscription.update { it + 1 }
}
