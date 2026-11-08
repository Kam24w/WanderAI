package com.vibra.wanderai.presentation.explore

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import com.vibra.wanderai.domain.usecase.MapFocusRequest
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.PlanSession
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val places: List<Place> = emptyList(),
    val activeItinerary: Itinerary? = null,
    val location: TravelerLocation = ObserveTravelerLocationUseCase.FALLBACK,
    val connection: VibraConnectionState = VibraConnectionState.Disconnected,
    val syncStatus: WatchSyncStatus = WatchSyncStatus.Idle,
    /** A Vibra watch is reachable through the Wear OS Data Layer. */
    val wearConnected: Boolean = false,
    val selectedPlaceId: String? = null,
    val showOnlyRecommended: Boolean = false,
    /** A recommendation tapped in the AI chat or in a generated itinerary, not yet shown. */
    val focusRequest: MapFocusRequest? = null,
) {
    /** Itinerary stops when a route is active; otherwise the top-rated places. */
    val recommendedIds: Set<String>
        get() = activeItinerary?.stops?.map { it.place.id }?.toSet()
            ?: places.filter { it.rating >= RECOMMENDED_RATING }.map(Place::id).toSet()

    val visiblePlaces: List<Place>
        get() = if (showOnlyRecommended) places.filter { it.id in recommendedIds } else places

    val selectedPlace: Place? get() = places.firstOrNull { it.id == selectedPlaceId }

    val isActiveItinerarySynced: Boolean
        get() = (syncStatus as? WatchSyncStatus.Synced)?.itineraryId == activeItinerary?.id && activeItinerary != null

    private companion object {
        const val RECOMMENDED_RATING = 4.7
    }
}

private data class ExploreLocalState(
    val selectedPlaceId: String? = null,
    val showOnlyRecommended: Boolean = false,
)

class ExploreViewModel(
    placesRepository: PlacesRepository,
    private val itineraryRepository: ItineraryRepository,
    private val vibraRepository: VibraRepository,
    private val syncItineraryToWatch: SyncItineraryToWatchUseCase,
    wearRepository: WearDataLayerRepository,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
    private val planSession: PlanSession,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val local = MutableStateFlow(ExploreLocalState())
    private val places = flow { emit(placesRepository.getPlaces()) }

    val state: StateFlow<ExploreUiState> = combine(
        combine(places, itineraryRepository.activeItinerary, location, ::Triple),
        combine(vibraRepository.connectionState, vibraRepository.syncStatus, wearRepository.connectedWatches, ::Triple),
        combine(local, planSession.mapFocus, ::Pair),
    ) { (placeList, itinerary, loc), (connection, sync, watches), (ui, focus) ->
        ExploreUiState(
            places = placeList,
            activeItinerary = itinerary,
            location = loc,
            connection = connection,
            syncStatus = sync,
            wearConnected = watches.isNotEmpty(),
            selectedPlaceId = ui.selectedPlaceId,
            showOnlyRecommended = ui.showOnlyRecommended,
            focusRequest = focus,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    fun selectPlace(placeId: String?) = local.update { it.copy(selectedPlaceId = placeId) }

    fun toggleRecommended() = local.update { it.copy(showOnlyRecommended = !it.showOnlyRecommended) }

    /** Selects the requested place (making sure its marker is visible) and marks the request done. */
    fun onFocusHandled(request: MapFocusRequest) {
        val recommended = state.value.recommendedIds
        local.update {
            it.copy(
                selectedPlaceId = request.placeId,
                showOnlyRecommended = it.showOnlyRecommended && request.placeId in recommended,
            )
        }
        planSession.consumeFocus(request)
    }

    fun clearRoute() {
        itineraryRepository.setActive(null)
        vibraRepository.updateSyncStatus(WatchSyncStatus.Idle)
    }

    fun syncToWatch() {
        val itinerary = state.value.activeItinerary ?: return
        val origin = state.value.location.point
        viewModelScope.launch { syncItineraryToWatch(itinerary, origin) }
    }
}
