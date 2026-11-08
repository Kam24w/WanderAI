package com.vibra.wanderai.presentation.explore

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import com.vibra.wanderai.domain.repository.RoutesRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import com.vibra.wanderai.domain.usecase.MapFocusRequest
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.PlanSession
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    /** Walking path of the active route (real streets when [RoutePath.followsStreets]). */
    val routePath: RoutePath = RoutePath.NONE,
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
    /** Google Places results opened from the chat: shown as extra markers. */
    val externalPlaces: List<Place> = emptyList(),
)

/** Polyline of the active route. Straight segments until the Routes API answers (or if it fails). */
data class RoutePath(val points: List<GeoPoint>, val followsStreets: Boolean) {
    companion object {
        val NONE = RoutePath(emptyList(), followsStreets = false)
    }
}

/** What makes a new Routes API request necessary: another itinerary or a move of ~110 m. */
private data class RouteRequest(val itineraryId: String, val stops: List<GeoPoint>, val originCell: Pair<Int, Int>)

class ExploreViewModel(
    placesRepository: PlacesRepository,
    private val itineraryRepository: ItineraryRepository,
    private val vibraRepository: VibraRepository,
    private val syncItineraryToWatch: SyncItineraryToWatchUseCase,
    wearRepository: WearDataLayerRepository,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
    private val planSession: PlanSession,
    private val routesRepository: RoutesRepository,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val local = MutableStateFlow(ExploreLocalState())
    private val places = flow { emit(placesRepository.getPlaces()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val routePath: Flow<RoutePath> =
        combine(itineraryRepository.activeItinerary, location) { itinerary, loc ->
            itinerary?.let { RouteRequest(it.id, it.stops.map { stop -> stop.place.location }, loc.point.cell()) }
        }
            .distinctUntilChanged()
            .transformLatest { request ->
                if (request == null) {
                    emit(RoutePath.NONE)
                    return@transformLatest
                }
                val waypoints = listOf(location.value.point) + request.stops
                emit(RoutePath(waypoints, followsStreets = false))
                routesRepository.walkingPath(waypoints).onSuccess { emit(RoutePath(it, followsStreets = true)) }
            }
            .onStart { emit(RoutePath.NONE) }

    val state: StateFlow<ExploreUiState> = combine(
        combine(places, itineraryRepository.activeItinerary, location, ::Triple),
        combine(vibraRepository.connectionState, vibraRepository.syncStatus, wearRepository.connectedWatches, ::Triple),
        combine(local, planSession.mapFocus, routePath, ::Triple),
    ) { (placeList, itinerary, loc), (connection, sync, watches), (ui, focus, path) ->
        val catalogueIds = placeList.map(Place::id).toSet()
        ExploreUiState(
            places = placeList + ui.externalPlaces.filterNot { it.id in catalogueIds },
            activeItinerary = itinerary,
            location = loc,
            connection = connection,
            syncStatus = sync,
            wearConnected = watches.isNotEmpty(),
            selectedPlaceId = ui.selectedPlaceId,
            showOnlyRecommended = ui.showOnlyRecommended,
            focusRequest = focus,
            routePath = path,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    fun selectPlace(placeId: String?) = local.update { it.copy(selectedPlaceId = placeId) }

    fun toggleRecommended() = local.update { it.copy(showOnlyRecommended = !it.showOnlyRecommended) }

    /** Selects the requested place (making sure its marker is visible) and marks the request done. */
    fun onFocusHandled(request: MapFocusRequest) {
        val recommended = state.value.recommendedIds
        val known = state.value.places.any { it.id == request.placeId }
        local.update {
            it.copy(
                selectedPlaceId = request.placeId,
                showOnlyRecommended = it.showOnlyRecommended && request.placeId in recommended,
                externalPlaces = if (known) it.externalPlaces else (it.externalPlaces + request.place).takeLast(MAX_EXTERNAL),
            )
        }
        planSession.consumeFocus(request)
    }

    private fun GeoPoint.cell(): Pair<Int, Int> = (latitude * 1_000).roundToInt() to (longitude * 1_000).roundToInt()

    private companion object {
        const val MAX_EXTERNAL = 20
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
