package com.vibra.wanderai.presentation.explore

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.PlaceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreVisibilityTest {

    private fun place(id: String, rating: Double = 4.0, source: PlaceSource = PlaceSource.CATALOGUE) =
        Place(id, id, PlaceCategory.MUSEUM, GeoPoint(1.21, -77.28), rating, 0, 30, LocalizedText("", ""), source = source)

    private val catalogue = listOf(place("a", 4.9), place("b", 4.8), place("c", 4.1), place("d", 3.9))

    @Test
    fun `map starts clean without filters, route or chat selections`() {
        assertTrue(ExploreUiState(places = catalogue).visiblePlaces.isEmpty())
    }

    @Test
    fun `filter chip shows the top rated catalogue places`() {
        val state = ExploreUiState(places = catalogue, showRecommended = true)
        assertEquals(listOf("a", "b"), state.visiblePlaces.map { it.id })
    }

    @Test
    fun `route stops, chat pins and the selected place are always shown`() {
        val google = place("g:x", 4.9, PlaceSource.GOOGLE)
        val route = Itinerary("r", listOf(ItineraryStop(catalogue[3], 5, 400)), 0)
        val state = ExploreUiState(
            places = catalogue + google,
            activeItinerary = route,
            pinnedPlaceIds = setOf("g:x"),
            selectedPlaceId = "c",
        )
        assertEquals(setOf("d", "g:x", "c"), state.visiblePlaces.map { it.id }.toSet())
        assertEquals(setOf("d"), state.routePlaceIds)
        assertTrue(state.isCourseTo("d"))
        // Google places never count as catalogue highlights.
        assertEquals(setOf("a", "b"), state.copy(showRecommended = true).topRatedIds)
    }
}
