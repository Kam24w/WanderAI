package com.vibra.wanderai.data.supabase

import android.util.Log
import com.vibra.wanderai.domain.model.AchievementType
import com.vibra.wanderai.domain.repository.AchievementsRepository
import com.vibra.wanderai.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SupabaseProfileRepository(private val client: SupabaseClient) : ProfileRepository {

    override suspend fun upsertProfile(displayName: String): Result<Unit> = runSupabase("upsertProfile") {
        client.from(SupabaseTables.PROFILES).upsert(ProfileRow(client.requireUserId(), displayName))
        Unit
    }
}

class SupabaseAchievementsRepository(private val client: SupabaseClient) : AchievementsRepository {

    private val _unlocked = MutableStateFlow<Set<AchievementType>>(emptySet())
    override val unlocked: StateFlow<Set<AchievementType>> = _unlocked.asStateFlow()

    override suspend fun refresh(): Result<Unit> = runSupabase("refreshAchievements") {
        val userId = client.requireUserId()
        val rows = client.from(SupabaseTables.USER_ACHIEVEMENTS)
            .select { filter { eq("user_id", userId) } }
            .decodeList<UserAchievementRow>()
        _unlocked.value = rows.mapNotNull { row ->
            AchievementType.entries.firstOrNull { it.name == row.achievementCode }
        }.toSet()
    }

    override suspend fun unlock(types: Set<AchievementType>): Result<Unit> {
        val missing = types - _unlocked.value
        if (missing.isEmpty()) return Result.success(Unit)
        return runSupabase("unlockAchievements") {
            val userId = client.requireUserId()
            client.from(SupabaseTables.USER_ACHIEVEMENTS).upsert(
                missing.map { UserAchievementRow(userId, it.name) },
            ) {
                onConflict = "user_id,achievement_code"
                ignoreDuplicates = true
            }
            _unlocked.update { it + missing }
        }
    }

    override fun clearLocal() {
        _unlocked.value = emptySet()
    }
}

internal suspend inline fun <T> runSupabase(operation: String, block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("WanderAISupabase", "Supabase $operation failed", e)
        Result.failure(e)
    }
