package com.vibra.wanderai.domain.model

data class ItineraryFilters(
    val availableMinutes: Int,
    val budgetCop: Int,
    val maxDistanceKm: Float,
    val categories: Set<PlaceCategory>,
) {
    companion object {
        val TIME_RANGE_MINUTES = 30..480
        const val TIME_STEP_MINUTES = 15
        val BUDGET_RANGE_COP = 10_000..250_000
        const val BUDGET_STEP_COP = 5_000
        val DISTANCE_RANGE_KM = 0.5f..5f

        val DEFAULT = ItineraryFilters(
            availableMinutes = 150,
            budgetCop = 80_000,
            maxDistanceKm = 2f,
            categories = setOf(PlaceCategory.RESTAURANT, PlaceCategory.MUSEUM),
        )
    }
}

data class ItineraryStop(
    val place: Place,
    /** Walking time from the previous stop (or from the origin for the first stop). */
    val walkingMinutes: Int,
    val distanceMeters: Int,
)

data class Itinerary(
    val id: String,
    val stops: List<ItineraryStop>,
    val createdAtMillis: Long,
) {
    val totalMinutes: Int get() = stops.sumOf { it.walkingMinutes + it.place.visitMinutes }
    val estimatedCostCop: Int get() = stops.sumOf { it.place.averageCostCop }
    val totalDistanceMeters: Int get() = stops.sumOf { it.distanceMeters }

    /** Category that best describes the itinerary (used for titles and thumbnails). */
    val dominantCategory: PlaceCategory
        get() = stops.groupingBy { it.place.category }.eachCount()
            .maxByOrNull { it.value }?.key ?: PlaceCategory.HERITAGE
}

sealed interface ItineraryGenerationResult {
    data class Success(val itinerary: Itinerary) : ItineraryGenerationResult
    data object NoMatches : ItineraryGenerationResult
}

/** A monument recognised near the traveler. */
data class LandmarkMatch(
    val place: Place,
    val distanceMeters: Int,
    val confidencePercent: Int,
)
