package com.vibra.wanderai.presentation.recommendations

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.ItineraryGenerationResult
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.usecase.GenerateItineraryUseCase
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.PlanSession
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class RecommendationsUiState(
    val filters: ItineraryFilters = ItineraryFilters.DEFAULT,
    val isGenerating: Boolean = false,
    val result: Itinerary? = null,
    val noMatches: Boolean = false,
    val isResultSaved: Boolean = false,
    val location: TravelerLocation = ObserveTravelerLocationUseCase.FALLBACK,
) {
    val canGenerate: Boolean get() = filters.categories.isNotEmpty() && !isGenerating
}

private data class FormState(
    val isGenerating: Boolean = false,
    val result: Itinerary? = null,
    val noMatches: Boolean = false,
)

class RecommendationsViewModel(
    private val generateItinerary: GenerateItineraryUseCase,
    private val itineraryRepository: ItineraryRepository,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
    /** Filters live here so the AI chat sends the same values the traveler picked. */
    private val planSession: PlanSession,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val form = MutableStateFlow(FormState())
    private val savedIds = itineraryRepository.savedItineraries.map { list -> list.map(Itinerary::id).toSet() }

    val state: StateFlow<RecommendationsUiState> =
        combine(form, planSession.filters, location, savedIds) { f, filters, loc, saved ->
            RecommendationsUiState(
                filters = filters,
                isGenerating = f.isGenerating,
                result = f.result,
                noMatches = f.noMatches,
                isResultSaved = f.result?.id in saved,
                location = loc,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecommendationsUiState())

    fun onTimeChange(minutes: Float) = updateFilters {
        val step = ItineraryFilters.TIME_STEP_MINUTES
        it.copy(availableMinutes = ((minutes / step).roundToInt() * step).coerceIn(ItineraryFilters.TIME_RANGE_MINUTES))
    }

    fun onBudgetChange(cop: Float) = updateFilters {
        val step = ItineraryFilters.BUDGET_STEP_COP
        it.copy(budgetCop = ((cop / step).roundToInt() * step).coerceIn(ItineraryFilters.BUDGET_RANGE_COP))
    }

    fun onDistanceChange(km: Float) = updateFilters {
        it.copy(maxDistanceKm = ((km * 10).roundToInt() / 10f).coerceIn(ItineraryFilters.DISTANCE_RANGE_KM))
    }

    /** Exact budget typed in the dialog: no slider step rounding, only clamped to the range. */
    fun setExactBudget(cop: Int) = updateFilters {
        it.copy(budgetCop = cop.coerceIn(ItineraryFilters.BUDGET_RANGE_COP))
    }

    /** Exact distance typed in the dialog, kept to 2 decimals (10 m). */
    fun setExactDistance(km: Float) = updateFilters {
        it.copy(maxDistanceKm = ((km * 100).roundToInt() / 100f).coerceIn(ItineraryFilters.DISTANCE_RANGE_KM))
    }

    fun toggleCategory(category: PlaceCategory) = updateFilters {
        it.copy(categories = if (category in it.categories) it.categories - category else it.categories + category)
    }

    fun generate() {
        if (!state.value.canGenerate) return
        val filters = planSession.filters.value
        val origin = location.value.point
        form.update { it.copy(isGenerating = true, noMatches = false) }
        viewModelScope.launch {
            when (val result = generateItinerary(origin, filters)) {
                is ItineraryGenerationResult.Success ->
                    form.update { it.copy(isGenerating = false, result = result.itinerary) }
                ItineraryGenerationResult.NoMatches ->
                    form.update { it.copy(isGenerating = false, noMatches = true) }
            }
        }
    }

    fun dismissResult() = form.update { it.copy(result = null) }

    fun dismissNoMatches() = form.update { it.copy(noMatches = false) }

    fun saveResult() {
        val itinerary = form.value.result ?: return
        viewModelScope.launch { itineraryRepository.save(itinerary) }
    }

    /** Makes the generated itinerary the active route on the map / watch. */
    fun activateResult() {
        val itinerary = form.value.result ?: return
        itineraryRepository.setActive(itinerary)
        form.update { it.copy(result = null) }
    }

    /** Activates the generated route and asks the map to fly to [placeId]. */
    fun showStopOnMap(placeId: String) {
        activateResult()
        planSession.focusPlace(placeId)
    }

    private fun updateFilters(transform: (ItineraryFilters) -> ItineraryFilters) {
        planSession.updateFilters(transform)
        form.update { it.copy(noMatches = false) }
    }
}
