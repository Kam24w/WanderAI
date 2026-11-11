package com.vibra.wanderai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelModeTest {

    @Test
    fun `vehicles are faster than walking`() {
        assertEquals(13, TravelMode.WALK.minutesFor(1_000))
        assertEquals(3, TravelMode.MOTORCYCLE.minutesFor(1_000))
        assertEquals(3, TravelMode.CAR.minutesFor(1_000))
        assertTrue(TravelMode.MOTORCYCLE.minutesFor(5_000) < TravelMode.WALK.minutesFor(5_000))
    }

    @Test
    fun `a short trip takes at least one minute`() {
        TravelMode.entries.forEach { assertEquals(1, it.minutesFor(0)) }
    }

    @Test
    fun `vehicles get wider margins before a U-turn cue`() {
        assertTrue(TravelMode.CAR.wrongWayMeters > TravelMode.WALK.wrongWayMeters)
        assertTrue(TravelMode.MOTORCYCLE.wrongWayMeters > TravelMode.WALK.wrongWayMeters)
        assertTrue(TravelMode.CAR.arrivalRadiusMeters > TravelMode.WALK.arrivalRadiusMeters)
    }

    @Test
    fun `unknown stored values fall back to walking`() {
        assertEquals(TravelMode.CAR, TravelMode.fromName("CAR"))
        assertEquals(TravelMode.WALK, TravelMode.fromName(null))
        assertEquals(TravelMode.WALK, TravelMode.fromName("BICYCLE"))
    }
}
