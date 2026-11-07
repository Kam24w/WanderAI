package com.vibra.wanderai.data.traveler

import com.vibra.wanderai.domain.model.Achievement
import com.vibra.wanderai.domain.model.AchievementType
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.TravelerStats
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.TravelerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Activity history is not tracked server-side yet, so this repository starts
 * from a reference baseline and adds what the traveler actually saved in the
 * app. Replace with a Supabase `traveler_stats` query when the table exists.
 */
class LocalTravelerRepository(
    private val itineraryRepository: ItineraryRepository,
) : TravelerRepository {

    override fun observeStats(): Flow<TravelerStats> =
        itineraryRepository.savedItineraries.map { saved ->
            val savedStops = saved.flatMap { it.stops }
            val savedKm = saved.sumOf { it.totalDistanceMeters } / 1_000f
            val categoryCount = savedStops.groupingBy { it.place.category }.eachCount()

            TravelerStats(
                trips = BASE_TRIPS + saved.size,
                placesVisited = BASE_PLACES + savedStops.map { it.place.id }.distinct().size,
                weeklyExplorationMinutes = BASE_WEEKLY_MINUTES,
                weeklyDistanceKm = BASE_WEEKLY_KM.dropLast(1) + (BASE_WEEKLY_KM.last() + savedKm),
                achievements = listOf(
                    Achievement(AchievementType.FIRST_ROUTE, if (saved.isNotEmpty()) 1f else 0f),
                    Achievement(
                        AchievementType.COLONIAL_EXPLORER,
                        progress(categoryCount[PlaceCategory.HERITAGE] ?: 0, base = 3, goal = 5),
                    ),
                    Achievement(
                        AchievementType.WALKER_50K,
                        ((BASE_WEEKLY_KM.sum() + savedKm) / 50f).coerceAtMost(1f),
                    ),
                    Achievement(
                        AchievementType.VIEWPOINT_HUNTER,
                        progress(categoryCount[PlaceCategory.VIEWPOINT] ?: 0, base = 1, goal = 3),
                    ),
                    Achievement(
                        AchievementType.FOODIE,
                        progress(categoryCount[PlaceCategory.RESTAURANT] ?: 0, base = 4, goal = 6),
                    ),
                ),
            )
        }

    private fun progress(count: Int, base: Int, goal: Int): Float =
        ((base + count).toFloat() / goal).coerceAtMost(1f)

    private companion object {
        const val BASE_TRIPS = 25
        const val BASE_PLACES = 110
        val BASE_WEEKLY_MINUTES = listOf(45, 80, 35, 120, 95, 180, 150)
        val BASE_WEEKLY_KM = listOf(6.2f, 8.4f, 5.1f, 9.8f, 7.3f, 11.6f)
    }
}
