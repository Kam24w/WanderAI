package com.vibra.wanderai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary                = LightPrimary,
    onPrimary              = LightOnPrimary,
    primaryContainer       = LightPrimaryContainer,
    onPrimaryContainer     = LightOnPrimaryContainer,
    secondary              = LightSecondary,
    onSecondary            = LightOnSecondary,
    secondaryContainer     = LightSecondaryContainer,
    onSecondaryContainer   = LightOnSecondaryContainer,
    tertiary               = LightTertiary,
    onTertiary             = LightOnTertiary,
    tertiaryContainer      = LightTertiaryContainer,
    onTertiaryContainer    = LightOnTertiaryContainer,
    error                  = LightError,
    onError                = LightOnError,
    errorContainer         = LightErrorContainer,
    onErrorContainer       = LightOnErrorContainer,
    background             = LightBackground,
    onBackground           = LightOnBackground,
    surface                = LightSurface,
    onSurface              = LightOnSurface,
    surfaceVariant         = LightSurfaceVariant,
    onSurfaceVariant       = LightOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFF3F6FA),
    surfaceContainer       = Color(0xFFEEF2F7),
    surfaceContainerHigh   = Color(0xFFE7ECF3),
    surfaceContainerHighest= Color(0xFFE1E8F0),
    outline                = LightOutline,
    outlineVariant         = LightOutlineVariant,
    scrim                  = LightScrim,
    inverseSurface         = LightInverseSurface,
    inverseOnSurface       = LightInverseOnSurface,
    inversePrimary         = LightInversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnTertiary,
    tertiaryContainer      = DarkTertiaryContainer,
    onTertiaryContainer    = DarkOnTertiaryContainer,
    error                  = DarkError,
    onError                = DarkOnError,
    errorContainer         = DarkErrorContainer,
    onErrorContainer       = DarkOnErrorContainer,
    background             = DarkBackground,
    onBackground           = DarkOnBackground,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0A1622),
    surfaceContainerLow    = Color(0xFF12202F),
    surfaceContainer       = Color(0xFF172636),
    surfaceContainerHigh   = Color(0xFF1C2C3E),
    surfaceContainerHighest= Color(0xFF223447),
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVariant,
    scrim                  = DarkScrim,
    inverseSurface         = DarkInverseSurface,
    inverseOnSurface       = DarkInverseOnSurface,
    inversePrimary         = DarkInversePrimary,
)

/**
 * Brand tokens that Material 3 has no slot for (map, charts, categories,
 * the navy "Watch Sync" card…). Read them through [WanderTheme.colors].
 */
@Immutable
data class WanderExtendedColors(
    val brandNavy: Color,
    val brandTeal: Color,
    val brandGold: Color,
    val accentWarm: Color,
    val success: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val syncCard: Color,
    val onSyncCard: Color,
    val onSyncCardMuted: Color,
    val mapLand: Color,
    val mapBlock: Color,
    val mapRoad: Color,
    val mapRoadMajor: Color,
    val mapWater: Color,
    val mapPark: Color,
    val mapRoute: Color,
    val chartGrid: Color,
    val categoryRestaurant: Color,
    val categoryPark: Color,
    val categoryMuseum: Color,
    val categoryViewpoint: Color,
    val categoryHeritage: Color,
    val isDark: Boolean,
)

private val LightExtended = WanderExtendedColors(
    brandNavy = NavyDeep,
    brandTeal = TealBrand,
    brandGold = GoldAccent,
    accentWarm = AmberSignal,
    success = SuccessGreen,
    heroStart = NavyDeep,
    heroEnd = NavyNight,
    syncCard = NavyDeep,
    onSyncCard = Color.White,
    onSyncCardMuted = Color.White.copy(alpha = 0.72f),
    mapLand = Color(0xFFF1EFE9),
    mapBlock = Color(0xFFE6E2D8),
    mapRoad = Color(0xFFFFFFFF),
    mapRoadMajor = Color(0xFFFBE3B6),
    mapWater = Color(0xFFAFD3EA),
    mapPark = Color(0xFFCDE8C9),
    mapRoute = AmberSignal,
    chartGrid = Color(0xFFE1E8F0),
    categoryRestaurant = RestaurantCoral,
    categoryPark = ParkGreen,
    categoryMuseum = MuseumPlum,
    categoryViewpoint = TealBrand,
    categoryHeritage = NavyDeep,
    isDark = false,
)

private val DarkExtended = WanderExtendedColors(
    brandNavy = NavyDeep,
    brandTeal = TealLight,
    brandGold = DarkTertiary,
    accentWarm = AmberSignal,
    success = SuccessGreenDark,
    heroStart = NavyDeep,
    heroEnd = DarkBackground,
    syncCard = Color(0xFF1E3558),
    onSyncCard = Color.White,
    onSyncCardMuted = Color.White.copy(alpha = 0.70f),
    mapLand = Color(0xFF16222F),
    mapBlock = Color(0xFF1C2A39),
    mapRoad = Color(0xFF2A3B4E),
    mapRoadMajor = Color(0xFF4A5A3A),
    mapWater = Color(0xFF1F4466),
    mapPark = Color(0xFF1E3A2E),
    mapRoute = AmberSignal,
    chartGrid = Color(0xFF2A3D52),
    categoryRestaurant = Color(0xFFF2936F),
    categoryPark = Color(0xFF6CCB98),
    categoryMuseum = Color(0xFFA68BE8),
    categoryViewpoint = TealLight,
    categoryHeritage = DarkPrimary,
    isDark = true,
)

val LocalWanderColors = staticCompositionLocalOf { LightExtended }

object WanderTheme {
    val colors: WanderExtendedColors
        @Composable get() = LocalWanderColors.current
}

@Composable
fun WanderAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(LocalWanderColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = Shapes,
            content     = content
        )
    }
}
