package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.TravelerLocation
import com.vibra.wanderai.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Live GPS position clamped to WanderAI's coverage area (San Juan de Pasto).
 * Outside coverage, or without permission, Pasto's centre is used so every
 * screen keeps working; [TravelerLocation.isLive] tells the UI which case applies.
 */
class ObserveTravelerLocationUseCase(
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Flow<TravelerLocation> =
        locationRepository.observeLocation()
            .map { fix ->
                if (fix.distanceTo(GeoPoint.PASTO_CENTER) <= COVERAGE_RADIUS_METERS) {
                    TravelerLocation(fix, isLive = true)
                } else {
                    FALLBACK
                }
            }
            .onStart { emit(FALLBACK) }
            .catch { emit(FALLBACK) }
            .distinctUntilChanged()

    companion object {
        private const val COVERAGE_RADIUS_METERS = 15_000.0
        val FALLBACK = TravelerLocation(GeoPoint.PASTO_CENTER, isLive = false)
    }
}
