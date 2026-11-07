package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.data.places.PastoPlacesRepository
import com.vibra.wanderai.domain.model.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentifyLandmarkUseCaseTest {

    private val identify = IdentifyLandmarkUseCase(PastoPlacesRepository())

    @Test
    fun `standing at San Felipe detects it with high confidence`() = runTest {
        val match = identify(GeoPoint(1.2147, -77.2822))
        assertEquals("san-felipe", match?.place?.id)
        assertTrue(match!!.confidencePercent >= 95)
    }

    @Test
    fun `far from the historic centre detects nothing`() = runTest {
        assertNull(identify(GeoPoint(4.7110, -74.0721))) // Bogotá
    }

    @Test
    fun `confidence decreases with distance`() = runTest {
        val near = identify(GeoPoint(1.2147, -77.2822))!!
        val far = identify(GeoPoint(1.2156, -77.2822))!! // ~100 m north of San Felipe
        assertEquals(near.place.id, far.place.id)
        assertTrue(far.confidencePercent < near.confidencePercent)
    }

    @Test
    fun `haversine distance is accurate`() {
        // 0.001° of latitude ≈ 110.5 m
        val d = GeoPoint(1.0, -77.0).distanceTo(GeoPoint(1.001, -77.0))
        assertEquals(111.2, d, 1.0)
    }
}
