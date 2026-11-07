package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.LandmarkMatch
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.repository.PlacesRepository
import kotlin.math.roundToInt

/**
 * Picks the monument the traveler is most likely looking at, using GPS
 * proximity. Confidence decays linearly with distance inside [MAX_RANGE_METERS].
 *
 * This is the seam where an on-device / cloud image classifier plugs in:
 * combine its label scores with this geographic prior.
 */
class IdentifyLandmarkUseCase(
    private val placesRepository: PlacesRepository,
) {
    suspend operator fun invoke(position: GeoPoint): LandmarkMatch? =
        placesRepository.getPlaces()
            .filter { it.category == PlaceCategory.HERITAGE || it.category == PlaceCategory.MUSEUM }
            .map { it to position.distanceTo(it.location) }
            .filter { (_, meters) -> meters <= MAX_RANGE_METERS }
            .minByOrNull { (_, meters) -> meters }
            ?.let { (place, meters) ->
                val confidence = (MAX_CONFIDENCE - (meters / MAX_RANGE_METERS) * CONFIDENCE_SPAN)
                    .roundToInt()
                    .coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE.toInt())
                LandmarkMatch(place, meters.roundToInt(), confidence)
            }

    private companion object {
        const val MAX_RANGE_METERS = 350.0
        const val MAX_CONFIDENCE = 98.0
        const val CONFIDENCE_SPAN = 48.0
        const val MIN_CONFIDENCE = 50
    }
}
