package com.vibra.wanderai.data.traveler

import com.vibra.wanderai.domain.model.TravelerStats
import com.vibra.wanderai.domain.repository.AchievementsRepository
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.TravelerRepository
import com.vibra.wanderai.domain.usecase.TravelerStatsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Profile statistics from the traveler's real activity: saved itineraries (local,
 * mirrored to Supabase) plus the achievements already unlocked in `user_achievements`.
 * Unlocking new achievements is done by [com.vibra.wanderai.domain.usecase.CloudSyncCoordinator].
 */
class LocalTravelerRepository(
    private val itineraryRepository: ItineraryRepository,
    private val achievementsRepository: AchievementsRepository,
    private val calculator: TravelerStatsCalculator = TravelerStatsCalculator(),
) : TravelerRepository {

    override fun observeStats(): Flow<TravelerStats> =
        combine(itineraryRepository.savedItineraries, achievementsRepository.unlocked) { saved, unlocked ->
            calculator.stats(saved, unlocked)
        }
}
