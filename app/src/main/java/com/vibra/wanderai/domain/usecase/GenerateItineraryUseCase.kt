package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryFilters
import com.vibra.wanderai.domain.model.ItineraryGenerationResult
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.repository.PlacesRepository
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Builds a walkable route that respects time, budget, radius and categories.
 *
 * Scoring favours well-rated places close to the current position, with a
 * penalty for repeating categories (greedy nearest-best). It is deterministic
 * and runs offline; a remote AI ranker can replace it behind the same signature.
 */
class GenerateItineraryUseCase(
    private val placesRepository: PlacesRepository,
) {
    suspend operator fun invoke(origin: GeoPoint, filters: ItineraryFilters): ItineraryGenerationResult {
        val wanted = filters.categories.flatMap { category ->
            if (category == PlaceCategory.MUSEUM) listOf(category, PlaceCategory.HERITAGE) else listOf(category)
        }.toSet()
        val radiusMeters = filters.maxDistanceKm * 1_000

        val candidates = placesRepository.getPlaces()
            .filter { it.category in wanted && origin.distanceTo(it.location) <= radiusMeters }
            .toMutableList()

        val stops = mutableListOf<ItineraryStop>()
        var position = origin
        var minutesLeft = filters.availableMinutes
        var budgetLeft = filters.budgetCop

        while (candidates.isNotEmpty() && stops.size < MAX_STOPS) {
            val (place, leg) = candidates
                .asSequence()
                .filter { it.averageCostCop <= budgetLeft }
                .map { it to legFor(position, it) }
                .filter { (candidate, leg) -> leg.minutes + candidate.visitMinutes <= minutesLeft }
                .maxByOrNull { (candidate, leg) -> score(candidate, leg.meters, stops) }
                ?: break

            stops += ItineraryStop(place, walkingMinutes = leg.minutes, distanceMeters = leg.meters)
            minutesLeft -= leg.minutes + place.visitMinutes
            budgetLeft -= place.averageCostCop
            position = place.location
            candidates.remove(place)
        }

        return if (stops.isEmpty()) {
            ItineraryGenerationResult.NoMatches
        } else {
            ItineraryGenerationResult.Success(
                Itinerary(
                    id = UUID.randomUUID().toString(),
                    stops = stops,
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    private data class Leg(val meters: Int, val minutes: Int)

    private fun legFor(from: GeoPoint, place: Place): Leg {
        // Street network ≈ 1.3 × straight line in Pasto's grid.
        val meters = (from.distanceTo(place.location) * STREET_FACTOR).roundToInt()
        return Leg(meters, ceil(meters / WALKING_METERS_PER_MINUTE).toInt())
    }

    private fun score(place: Place, meters: Int, chosen: List<ItineraryStop>): Double {
        val repeats = chosen.count { it.place.category == place.category }
        return place.rating * 2.0 - meters / 400.0 - repeats * 1.5
    }

    private companion object {
        const val MAX_STOPS = 6
        const val STREET_FACTOR = 1.3
        const val WALKING_METERS_PER_MINUTE = 75.0
    }
}
