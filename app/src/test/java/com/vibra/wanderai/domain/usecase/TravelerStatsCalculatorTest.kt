package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.AchievementType
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TravelerStatsCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** Wednesday 2026-11-04 12:00 UTC. */
    private val now = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 4, 12, 0)
    }.timeInMillis
    private val calculator = TravelerStatsCalculator(clock = { now }, timeZone = utc)

    private fun place(id: String, category: PlaceCategory) = Place(
        id, id, category, GeoPoint(1.21, -77.28), 4.5, 10_000, 30, LocalizedText("", ""),
    )

    private fun itinerary(id: String, createdAt: Long, vararg places: Place, meters: Int = 1_000) =
        Itinerary(id, places.map { ItineraryStop(it, walkingMinutes = 10, distanceMeters = meters) }, createdAt)

    @Test
    fun `no activity means nothing unlocked and zero stats`() {
        val stats = calculator.stats(emptyList())
        assertEquals(0, stats.trips)
        assertEquals(0, stats.placesVisited)
        assertTrue(stats.achievements.none { it.isUnlocked })
        assertEquals(AchievementType.entries.size, stats.achievements.size)
        assertEquals(List(7) { 0 }, stats.weeklyExplorationMinutes)
    }

    @Test
    fun `first saved itinerary unlocks first route and counts distinct places`() {
        val museum = place("museo", PlaceCategory.MUSEUM)
        val saved = listOf(itinerary("a", now, museum, museum))
        val stats = calculator.stats(saved)
        assertEquals(1, stats.trips)
        assertEquals(1, stats.placesVisited)
        assertTrue(stats.achievements.first { it.type == AchievementType.FIRST_ROUTE }.isUnlocked)
        assertFalse(stats.achievements.first { it.type == AchievementType.FOODIE }.isUnlocked)
    }

    @Test
    fun `category achievements count distinct places only`() {
        val viewpoints = (1..3).map { place("mirador$it", PlaceCategory.VIEWPOINT) }
        val sameViewpointTwice = listOf(itinerary("a", now, viewpoints[0]), itinerary("b", now, viewpoints[0]))
        val hunter = { list: List<Itinerary> ->
            calculator.achievements(list).first { it.type == AchievementType.VIEWPOINT_HUNTER }
        }
        assertEquals(1f / 3f, hunter(sameViewpointTwice).progress, 0.001f)
        assertTrue(hunter(listOf(itinerary("c", now, *viewpoints.toTypedArray()))).isUnlocked)
    }

    @Test
    fun `remote unlocks stay unlocked after deleting itineraries`() {
        val stats = calculator.stats(emptyList(), unlockedRemotely = setOf(AchievementType.WALKER_50K))
        assertTrue(stats.achievements.first { it.type == AchievementType.WALKER_50K }.isUnlocked)
    }

    @Test
    fun `weekly charts bucket by weekday and by week`() {
        val day = 24L * 60 * 60 * 1_000
        val stop = place("p", PlaceCategory.PARK)
        val saved = listOf(
            itinerary("today", now, stop, meters = 2_000), // Wednesday, this week
            itinerary("monday", now - 2 * day, stop),       // Monday, this week
            itinerary("old", now - 15 * day, stop),         // 2 weeks ago, outside the weekday chart
        )
        val stats = calculator.stats(saved)
        val minutesPerItinerary = 10 + 30
        assertEquals(minutesPerItinerary, stats.weeklyExplorationMinutes[0]) // Monday
        assertEquals(minutesPerItinerary, stats.weeklyExplorationMinutes[2]) // Wednesday
        assertEquals(2 * minutesPerItinerary, stats.weeklyExplorationMinutes.sum())

        assertEquals(TravelerStatsCalculator.WEEKS, stats.weeklyDistanceKm.size)
        assertEquals(3f, stats.weeklyDistanceKm.last(), 0.001f)
        assertEquals(1f, stats.weeklyDistanceKm[TravelerStatsCalculator.WEEKS - 3], 0.001f)
    }
}
