package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.Achievement
import com.vibra.wanderai.domain.model.AchievementType
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.TravelerStats
import java.util.Calendar
import java.util.TimeZone

/**
 * Builds the profile statistics and achievement progress from what the traveler
 * really did in the app (saved itineraries), with no reference baseline.
 *
 * Achievements already unlocked in Supabase stay unlocked even if the itineraries
 * that earned them are deleted later.
 */
class TravelerStatsCalculator(
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {

    fun stats(saved: List<Itinerary>, unlockedRemotely: Set<AchievementType> = emptySet()): TravelerStats =
        TravelerStats(
            trips = saved.size,
            placesVisited = saved.flatMap { it.stops }.map { it.place.id }.distinct().size,
            weeklyExplorationMinutes = weeklyMinutes(saved),
            weeklyDistanceKm = weeklyDistanceKm(saved),
            achievements = achievements(saved).map {
                if (it.type in unlockedRemotely) it.copy(progress = 1f) else it
            },
        )

    /** Progress of every achievement computed from [saved]. */
    fun achievements(saved: List<Itinerary>): List<Achievement> {
        val stops = saved.flatMap { it.stops }
        val distinctByCategory = stops.map { it.place }.distinctBy { it.id }.groupingBy { it.category }.eachCount()
        val km = saved.sumOf { it.totalDistanceMeters } / 1_000f
        return AchievementType.entries.map { type ->
            val progress = when (type) {
                AchievementType.FIRST_ROUTE -> saved.size.toFloat() / GOAL_FIRST_ROUTE
                AchievementType.COLONIAL_EXPLORER ->
                    (distinctByCategory[PlaceCategory.HERITAGE] ?: 0).toFloat() / GOAL_HERITAGE
                AchievementType.WALKER_50K -> km / GOAL_KM
                AchievementType.VIEWPOINT_HUNTER ->
                    (distinctByCategory[PlaceCategory.VIEWPOINT] ?: 0).toFloat() / GOAL_VIEWPOINTS
                AchievementType.FOODIE ->
                    (distinctByCategory[PlaceCategory.RESTAURANT] ?: 0).toFloat() / GOAL_RESTAURANTS
            }
            Achievement(type, progress.coerceIn(0f, 1f))
        }
    }

    /** Planned exploration minutes per weekday (Monday first) over the last 7 days. */
    private fun weeklyMinutes(saved: List<Itinerary>): List<Int> {
        val minutes = IntArray(7)
        val since = clock() - WEEK_MS
        saved.filter { it.createdAtMillis >= since }.forEach { itinerary ->
            val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = itinerary.createdAtMillis }
            // Calendar: SUNDAY = 1 … SATURDAY = 7 → Monday-first index.
            val index = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            minutes[index] += itinerary.totalMinutes
        }
        return minutes.toList()
    }

    /** Kilometres per week for the last [WEEKS] weeks, oldest first. */
    private fun weeklyDistanceKm(saved: List<Itinerary>): List<Float> {
        val km = FloatArray(WEEKS)
        val now = clock()
        saved.forEach { itinerary ->
            val weeksAgo = ((now - itinerary.createdAtMillis) / WEEK_MS).toInt()
            if (weeksAgo in 0 until WEEKS) km[WEEKS - 1 - weeksAgo] += itinerary.totalDistanceMeters / 1_000f
        }
        return km.toList()
    }

    companion object {
        const val WEEKS = 6
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1_000

        const val GOAL_FIRST_ROUTE = 1
        const val GOAL_HERITAGE = 5
        const val GOAL_KM = 50f
        const val GOAL_VIEWPOINTS = 3
        const val GOAL_RESTAURANTS = 6
    }
}
