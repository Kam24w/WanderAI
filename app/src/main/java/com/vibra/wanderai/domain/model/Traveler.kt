package com.vibra.wanderai.domain.model

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}

enum class SignUpResult { SESSION_STARTED, CONFIRMATION_REQUIRED }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en"),
}

enum class AchievementType {
    FIRST_ROUTE,
    COLONIAL_EXPLORER,
    WALKER_50K,
    VIEWPOINT_HUNTER,
    FOODIE,
}

data class Achievement(
    val type: AchievementType,
    /** 0f..1f */
    val progress: Float,
) {
    val isUnlocked: Boolean get() = progress >= 1f
}

data class TravelerStats(
    val trips: Int,
    val placesVisited: Int,
    /** Minutes explored per weekday, Monday first (7 values). */
    val weeklyExplorationMinutes: List<Int>,
    /** Kilometres walked in each of the last weeks, oldest first. */
    val weeklyDistanceKm: List<Float>,
    val achievements: List<Achievement>,
)
