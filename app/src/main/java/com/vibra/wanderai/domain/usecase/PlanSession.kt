package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.ItineraryFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide state shared between screens:
 *  - the filters picked on the Plan screen (also sent to the AI chat);
 *  - the place the map must fly to when a recommendation is tapped in the chat
 *    or in a generated itinerary.
 */
class PlanSession {

    private val _filters = MutableStateFlow(ItineraryFilters.DEFAULT)
    val filters: StateFlow<ItineraryFilters> = _filters.asStateFlow()

    private val _mapFocus = MutableStateFlow<MapFocusRequest?>(null)
    val mapFocus: StateFlow<MapFocusRequest?> = _mapFocus.asStateFlow()

    private var focusSequence = 0L

    fun updateFilters(transform: (ItineraryFilters) -> ItineraryFilters) = _filters.update(transform)

    /** Asks the map to move to [placeId]; every call is a new request, even for the same place. */
    fun focusPlace(placeId: String) {
        _mapFocus.value = MapFocusRequest(placeId, ++focusSequence)
    }

    /** Called by the map once it has handled [request]. */
    fun consumeFocus(request: MapFocusRequest) {
        _mapFocus.update { if (it == request) null else it }
    }
}

data class MapFocusRequest(val placeId: String, val sequence: Long)
