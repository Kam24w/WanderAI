package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.usecase.LiveGuidanceTracker.Update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveGuidanceTrackerTest {

    private val destination = GeoPoint(1.2136, -77.2811)

    /** [meters] south of the destination (1e-5° of latitude ≈ 1.11 m). */
    private fun south(meters: Double) = GeoPoint(destination.latitude - meters / 111_195.0, destination.longitude)

    private fun List<Update>.cues() = filterIsInstance<Update.Cue>().map { it.action }
    private fun List<Update>.remaining() = filterIsInstance<Update.RemainingDistance>().map { it.meters }

    @Test
    fun `remaining distance is scaled by the initial street detour and reported in steps`() {
        val tracker = LiveGuidanceTracker(destination, initialRouteMeters = 600)

        assertEquals(listOf(600), tracker.onLocation(south(400.0)).remaining()) // factor 1.5
        assertTrue(tracker.onLocation(south(396.0)).remaining().isEmpty()) // 594 m: < 10 m change
        assertEquals(listOf(585), tracker.onLocation(south(390.0)).remaining())
    }

    @Test
    fun `walking away triggers one u turn and coming back confirms straight`() {
        val tracker = LiveGuidanceTracker(destination, initialRouteMeters = 300)
        tracker.onLocation(south(300.0))
        assertTrue(tracker.onLocation(south(330.0)).cues().isEmpty())

        assertEquals(listOf(NavigationAction.U_TURN), tracker.onLocation(south(345.0)).cues())
        assertTrue("no repeated U-turn", tracker.onLocation(south(360.0)).cues().isEmpty())

        val back = tracker.onLocation(south(325.0)).filterIsInstance<Update.Cue>()
        assertEquals(listOf(NavigationAction.STRAIGHT), back.map { it.action })
    }

    @Test
    fun `arrival fires once and then the tracker goes quiet`() {
        val tracker = LiveGuidanceTracker(destination, initialRouteMeters = 120)
        tracker.onLocation(south(100.0))

        val arrival = tracker.onLocation(south(15.0))
        assertEquals(listOf(NavigationAction.ARRIVED), arrival.cues())
        assertEquals(listOf(0), arrival.remaining())
        assertTrue(tracker.arrived)
        assertTrue(tracker.onLocation(south(5.0)).isEmpty())
    }
}
