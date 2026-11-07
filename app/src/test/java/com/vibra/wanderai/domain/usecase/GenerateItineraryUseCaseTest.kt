package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.data.places.PastoPlacesRepository
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.ItineraryGenerationResult
import com.vibra.wanderai.domain.model.PlaceCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateItineraryUseCaseTest {

    private val generate = GenerateItineraryUseCase(PastoPlacesRepository())
    private val origin = GeoPoint.PASTO_CENTER

    @Test
    fun `respects time, budget, radius and categories`() = runTest {
        val filters = ItineraryFilters(
            availableMinutes = 150,
            budgetCop = 40_000,
            maxDistanceKm = 1.5f,
            categories = setOf(PlaceCategory.RESTAURANT, PlaceCategory.MUSEUM),
        )

        val result = generate(origin, filters)

        assertTrue(result is ItineraryGenerationResult.Success)
        val itinerary = (result as ItineraryGenerationResult.Success).itinerary
        assertTrue(itinerary.stops.isNotEmpty())
        assertTrue(itinerary.totalMinutes <= filters.availableMinutes)
        assertTrue(itinerary.estimatedCostCop <= filters.budgetCop)
        val allowed = setOf(PlaceCategory.RESTAURANT, PlaceCategory.MUSEUM, PlaceCategory.HERITAGE)
        itinerary.stops.forEach { stop ->
            assertTrue(stop.place.category in allowed)
            assertTrue(origin.distanceTo(stop.place.location) <= 1_500.0)
        }
        assertEquals(itinerary.stops.size, itinerary.stops.map { it.place.id }.distinct().size)
    }

    @Test
    fun `returns NoMatches when nothing fits`() = runTest {
        val filters = ItineraryFilters(
            availableMinutes = 30,
            budgetCop = 10_000,
            maxDistanceKm = 0.5f,
            categories = setOf(PlaceCategory.VIEWPOINT),
        )
        assertEquals(ItineraryGenerationResult.NoMatches, generate(origin, filters))
    }

    @Test
    fun `museum filter also brings heritage monuments`() = runTest {
        val filters = ItineraryFilters.DEFAULT.copy(
            availableMinutes = 480,
            maxDistanceKm = 5f,
            categories = setOf(PlaceCategory.MUSEUM),
        )
        val itinerary = (generate(origin, filters) as ItineraryGenerationResult.Success).itinerary
        assertTrue(itinerary.stops.any { it.place.category == PlaceCategory.HERITAGE })
    }
}
