package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.repository.AchievementsRepository
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps Supabase in step with the signed-in traveler for the whole process lifetime:
 *  1. on sign-in: upserts `profiles`, merges `saved_itineraries` and loads `user_achievements`;
 *  2. while signed in: every change in the saved itineraries re-evaluates the achievements
 *     and records the newly unlocked ones (real user actions, no baseline).
 */
class CloudSyncCoordinator(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val itineraryRepository: ItineraryRepository,
    private val achievementsRepository: AchievementsRepository,
    private val calculator: TravelerStatsCalculator = TravelerStatsCalculator(),
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            authRepository.authState
                .map { (it as? AuthState.Authenticated)?.user }
                .distinctUntilChanged()
                .collectLatest { user ->
                    if (user == null) {
                        achievementsRepository.clearLocal()
                        return@collectLatest
                    }
                    // The profile row must exist before user_achievements / saved_itineraries reference it.
                    profileRepository.upsertProfile(user.displayName)
                    itineraryRepository.refreshFromCloud()
                    achievementsRepository.refresh()

                    itineraryRepository.savedItineraries.collectLatest { saved ->
                        val earned = calculator.achievements(saved).filter { it.isUnlocked }.map { it.type }.toSet()
                        achievementsRepository.unlock(earned)
                    }
                }
        }
    }
}

/** GDPR "delete account and data": cloud cascade first, then everything on the device. */
class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
    private val itineraryRepository: ItineraryRepository,
    private val achievementsRepository: AchievementsRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        authRepository.deleteAccount().onSuccess {
            itineraryRepository.clearLocal()
            achievementsRepository.clearLocal()
        }
}
