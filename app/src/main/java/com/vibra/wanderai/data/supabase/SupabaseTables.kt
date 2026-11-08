package com.vibra.wanderai.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Filas de las tablas definidas en el SQL de `supabase/migrations`.
 * Las políticas RLS limitan cada consulta a las filas de `auth.uid()`.
 */
internal object SupabaseTables {
    const val PROFILES = "profiles"
    const val ACHIEVEMENTS = "achievements"
    const val USER_ACHIEVEMENTS = "user_achievements"
    const val SAVED_ITINERARIES = "saved_itineraries"

    /** RPC `security definer` que borra `auth.users` del llamante; el resto cae en cascada. */
    const val RPC_DELETE_ACCOUNT = "delete_user_account"
}

@Serializable
internal data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
internal data class UserAchievementRow(
    @SerialName("user_id") val userId: String,
    @SerialName("achievement_code") val achievementCode: String,
)

@Serializable
internal data class SavedItineraryRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at_ms") val createdAtMs: Long,
    @SerialName("total_minutes") val totalMinutes: Int,
    @SerialName("total_distance_m") val totalDistanceM: Int,
    @SerialName("estimated_cost_cop") val estimatedCostCop: Int,
    val stops: List<SavedStopRow>,
)

@Serializable
internal data class SavedStopRow(
    @SerialName("place_id") val placeId: String,
    @SerialName("walk_min") val walkMin: Int,
    @SerialName("dist_m") val distM: Int,
)

internal fun SupabaseClient.currentUserId(): String? = auth.currentUserOrNull()?.id

internal fun SupabaseClient.requireUserId(): String =
    currentUserId() ?: throw IllegalStateException("No hay sesión de Supabase")
