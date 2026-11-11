package com.vibra.wanderai.presentation.explore

import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceSource
import com.vibra.wanderai.domain.model.TravelMode
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import com.vibra.wanderai.domain.repository.RoutesRepository
import com.vibra.wanderai.domain.repository.SettingsRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import com.vibra.wanderai.domain.usecase.MapFocusRequest
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.PlanSession
import com.vibra.wanderai.domain.usecase.StartNavigationToPlaceUseCase
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
import com.vibra.wanderai.presentation.common.LocationAwareViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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
    /** "Pines recomendados IA" chip: also shows the top-rated catalogue places. */
    val showRecommended: Boolean = false,
    /** Places opened from the AI chat or a generated itinerary (kept on the map). */
    val pinnedPlaceIds: Set<String> = emptySet(),
    /** A recommendation tapped in the AI chat or in a generated itinerary, not yet shown. */
    val focusRequest: MapFocusRequest? = null,
    /** Path of the active route in its travel mode (real streets when [RoutePath.followsStreets]). */
    val routePath: RoutePath = RoutePath.NONE,
    val course: CourseState = CourseState.Idle,
    /** The camera should frame the new course once its path is drawn. */
    val pendingRouteFit: Boolean = false,
    /** On foot, by motorcycle or by car: used by the next "Fijar rumbo". */
    val travelMode: TravelMode = TravelMode.DEFAULT,
) {
    /** Stops of the active route: the other markers are dimmed while it is shown. */
    val routePlaceIds: Set<String> get() = activeItinerary?.stops?.map { it.place.id }?.toSet().orEmpty()

    val topRatedIds: Set<String>
        get() = places.filter { it.source == PlaceSource.CATALOGUE && it.rating >= RECOMMENDED_RATING }.map(Place::id).toSet()

    /**
     * The map starts clean (only the traveler). Markers appear for the active route,
     * places opened from the chat / itinerary, the selected place, and the catalogue
     * highlights when the traveler turns the filter on.
     */
    val visiblePlaces: List<Place>
        get() {
            val ids = routePlaceIds + pinnedPlaceIds + listOfNotNull(selectedPlaceId) +
                (if (showRecommended) topRatedIds else emptySet())
            return places.filter { it.id in ids }
        }

    /** The active route is the course set towards [placeId]. */
    fun isCourseTo(placeId: String): Boolean = activeItinerary?.stops?.singleOrNull()?.place?.id == placeId

    /** The course to [placeId] is active and uses the selected mode (otherwise it can be set again). */
    fun isCurrentCourseTo(placeId: String): Boolean = isCourseTo(placeId) && activeItinerary?.travelMode == travelMode

    val selectedPlace: Place? get() = places.firstOrNull { it.id == selectedPlaceId }

    val isActiveItinerarySynced: Boolean
        get() = (syncStatus as? WatchSyncStatus.Synced)?.itineraryId == activeItinerary?.id && activeItinerary != null

    private companion object {
        const val RECOMMENDED_RATING = 4.7
    }
}

private data class ExploreLocalState(
    val selectedPlaceId: String? = null,
    val showRecommended: Boolean = false,
    val pinnedPlaceIds: Set<String> = emptySet(),
    /** Google Places results opened from the chat: not in the catalogue, so kept here. */
    val externalPlaces: List<Place> = emptyList(),
    val course: CourseState = CourseState.Idle,
    val pendingRouteFit: Boolean = false,
    val travelMode: TravelMode = TravelMode.DEFAULT,
)

/** Progress of "Fijar rumbo" for the selected place. */
sealed interface CourseState {
    data object Idle : CourseState
    data class Starting(val placeId: String) : CourseState
    data class Started(val placeId: String, val followsStreets: Boolean, val watchSynced: Boolean) : CourseState
}

/** Polyline of the active route. Straight segments until the Routes API answers (or if it fails). */
data class RoutePath(val points: List<GeoPoint>, val followsStreets: Boolean) {
    companion object {
        val NONE = RoutePath(emptyList(), followsStreets = false)
    }
}

/** What makes a new Routes API request necessary: another itinerary or a move of ~110 m. */
private data class RouteRequest(
    val itineraryId: String,
    val stops: List<GeoPoint>,
    val originCell: Pair<Int, Int>,
    val mode: TravelMode,
)

class ExploreViewModel(
    placesRepository: PlacesRepository,
    private val itineraryRepository: ItineraryRepository,
    private val vibraRepository: VibraRepository,
    private val syncItineraryToWatch: SyncItineraryToWatchUseCase,
    wearRepository: WearDataLayerRepository,
    observeTravelerLocation: ObserveTravelerLocationUseCase,
    private val planSession: PlanSession,
    private val routesRepository: RoutesRepository,
    private val startNavigationToPlace: StartNavigationToPlaceUseCase,
    private val settingsRepository: SettingsRepository,
) : LocationAwareViewModel(observeTravelerLocation) {

    private val local = MutableStateFlow(ExploreLocalState())
    private val places = flow { emit(placesRepository.getPlaces()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val routePath: Flow<RoutePath> =
        combine(itineraryRepository.activeItinerary, location) { itinerary, loc ->
            itinerary?.let { RouteRequest(it.id, it.stops.map { stop -> stop.place.location }, loc.point.cell(), it.travelMode) }
        }
            .distinctUntilChanged()
            .transformLatest { request ->
                if (request == null) {
                    emit(RoutePath.NONE)
                    return@transformLatest
                }
                val waypoints = listOf(location.value.point) + request.stops
                emit(RoutePath(waypoints, followsStreets = false))
                routesRepository.streetPath(waypoints, request.mode).onSuccess { emit(RoutePath(it, followsStreets = true)) }
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
            showRecommended = ui.showRecommended,
            pinnedPlaceIds = ui.pinnedPlaceIds,
            focusRequest = focus,
            routePath = path,
            course = ui.course,
            pendingRouteFit = ui.pendingRouteFit,
            travelMode = ui.travelMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    init {
        // The last mode the traveler picked is preselected (walking the first time).
        viewModelScope.launch {
            settingsRepository.travelMode.collect { mode -> local.update { it.copy(travelMode = mode) } }
        }
        // The route can also end outside this screen (e.g. STOP_NAVIGATION from the watch):
        // drop the course banner when the active itinerary goes away.
        viewModelScope.launch {
            itineraryRepository.activeItinerary
                .map { it == null }
                .distinctUntilChanged()
                .drop(1)
                .filter { it }
                .collect { local.update { it.copy(course = CourseState.Idle, pendingRouteFit = false) } }
        }
    }

    fun selectPlace(placeId: String?) = local.update { it.copy(selectedPlaceId = placeId) }

    /** Picks how the next course is calculated and remembers it for later. */
    fun selectTravelMode(mode: TravelMode) {
        if (local.value.course is CourseState.Starting) return
        local.update { it.copy(travelMode = mode) }
        viewModelScope.launch { settingsRepository.setTravelMode(mode) }
    }

    fun toggleRecommended() = local.update { it.copy(showRecommended = !it.showRecommended) }

    /**
     * "Fijar rumbo": street route from the traveler to [place] for the selected travel
     * mode, active navigation session and first cue + vibration sent to Vibra.
     */
    fun setCourse(place: Place) {
        if (local.value.course is CourseState.Starting) return
        local.update {
            it.copy(course = CourseState.Starting(place.id), pinnedPlaceIds = it.pinnedPlaceIds + place.id)
        }
        viewModelScope.launch {
            val result = startNavigationToPlace(place, state.value.location.point, local.value.travelMode)
            local.update {
                it.copy(
                    course = CourseState.Started(place.id, result.followsStreets, result.watchSynced),
                    pendingRouteFit = true,
                )
            }
        }
    }

    fun onRouteFitted() = local.update { it.copy(pendingRouteFit = false) }

    /** Selects the requested place (making sure its marker is visible) and marks the request done. */
    fun onFocusHandled(request: MapFocusRequest) {
        val known = state.value.places.any { it.id == request.placeId }
        local.update {
            it.copy(
                selectedPlaceId = request.placeId,
                pinnedPlaceIds = it.pinnedPlaceIds + request.placeId,
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
        local.update { it.copy(course = CourseState.Idle, pendingRouteFit = false) }
        itineraryRepository.setActive(null)
        vibraRepository.updateSyncStatus(WatchSyncStatus.Idle)
    }

    fun syncToWatch() {
        val itinerary = state.value.activeItinerary ?: return
        val origin = state.value.location.point
        viewModelScope.launch { syncItineraryToWatch(itinerary, origin) }
    }
}
