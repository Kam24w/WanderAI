package com.vibra.wanderai.presentation.vision

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.LandmarkMatch
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.usecase.IdentifyLandmarkUseCase
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class VisionUiState(
    val location: TravelerLocation = ObserveTravelerLocationUseCase.FALLBACK,
    val match: LandmarkMatch? = null,
    val isAnalyzing: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class VisionViewModel(
    observeTravelerLocation: ObserveTravelerLocationUseCase,
    private val identifyLandmark: IdentifyLandmarkUseCase,
) : LocationAwareViewModel(observeTravelerLocation) {

    val state: StateFlow<VisionUiState> =
        location
            .mapLatest { loc -> VisionUiState(location = loc, match = identifyLandmark(loc.point), isAnalyzing = false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisionUiState())
}
