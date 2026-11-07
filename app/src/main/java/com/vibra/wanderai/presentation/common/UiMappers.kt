package com.vibra.wanderai.presentation.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Straight
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.AchievementType
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.ui.theme.WanderTheme

// ── Place categories ──────────────────────────────────────────────────────────

val PlaceCategory.icon: ImageVector
    get() = when (this) {
        PlaceCategory.RESTAURANT -> Icons.Outlined.Restaurant
        PlaceCategory.PARK -> Icons.Outlined.Park
        PlaceCategory.MUSEUM -> Icons.Outlined.Museum
        PlaceCategory.VIEWPOINT -> Icons.Outlined.Landscape
        PlaceCategory.HERITAGE -> Icons.Outlined.Church
    }

@get:StringRes
val PlaceCategory.labelRes: Int
    get() = when (this) {
        PlaceCategory.RESTAURANT -> R.string.category_restaurant
        PlaceCategory.PARK -> R.string.category_park
        PlaceCategory.MUSEUM -> R.string.category_museum
        PlaceCategory.VIEWPOINT -> R.string.category_viewpoint
        PlaceCategory.HERITAGE -> R.string.category_heritage
    }

/** Title used for itineraries dominated by this category. */
@get:StringRes
val PlaceCategory.itineraryTitleRes: Int
    get() = when (this) {
        PlaceCategory.RESTAURANT -> R.string.itinerary_title_food
        PlaceCategory.PARK -> R.string.itinerary_title_nature
        PlaceCategory.MUSEUM, PlaceCategory.HERITAGE -> R.string.itinerary_title_culture
        PlaceCategory.VIEWPOINT -> R.string.itinerary_title_views
    }

@Composable
fun PlaceCategory.color(): Color {
    val colors = WanderTheme.colors
    return when (this) {
        PlaceCategory.RESTAURANT -> colors.categoryRestaurant
        PlaceCategory.PARK -> colors.categoryPark
        PlaceCategory.MUSEUM -> colors.categoryMuseum
        PlaceCategory.VIEWPOINT -> colors.categoryViewpoint
        PlaceCategory.HERITAGE -> colors.categoryHeritage
    }
}

// ── Achievements ──────────────────────────────────────────────────────────────

val AchievementType.icon: ImageVector
    get() = when (this) {
        AchievementType.FIRST_ROUTE -> Icons.Outlined.Route
        AchievementType.COLONIAL_EXPLORER -> Icons.Outlined.AccountBalance
        AchievementType.WALKER_50K -> Icons.AutoMirrored.Outlined.DirectionsWalk
        AchievementType.VIEWPOINT_HUNTER -> Icons.Outlined.Landscape
        AchievementType.FOODIE -> Icons.Outlined.Restaurant
    }

@get:StringRes
val AchievementType.labelRes: Int
    get() = when (this) {
        AchievementType.FIRST_ROUTE -> R.string.achievement_first_route
        AchievementType.COLONIAL_EXPLORER -> R.string.achievement_colonial
        AchievementType.WALKER_50K -> R.string.achievement_walker
        AchievementType.VIEWPOINT_HUNTER -> R.string.achievement_viewpoints
        AchievementType.FOODIE -> R.string.achievement_foodie
    }

// ── Vibra ─────────────────────────────────────────────────────────────────────

@get:StringRes
val VibraError.messageRes: Int
    get() = when (this) {
        VibraError.BLUETOOTH_UNAVAILABLE -> R.string.vibra_error_unavailable
        VibraError.BLUETOOTH_DISABLED -> R.string.vibra_error_disabled
        VibraError.PERMISSION_DENIED -> R.string.vibra_error_permission
        VibraError.NO_DEVICES_FOUND -> R.string.vibra_error_no_devices
        VibraError.CONNECTION_LOST -> R.string.vibra_error_connection_lost
        VibraError.PROTOCOL_UNSUPPORTED -> R.string.vibra_error_protocol
        VibraError.WRITE_FAILED -> R.string.vibra_error_write
        VibraError.NOT_CONNECTED -> R.string.vibra_error_not_connected
    }

val HapticCommand.icon: ImageVector
    get() = when (this) {
        is HapticCommand.Turn ->
            if (direction == TurnDirection.LEFT) Icons.Outlined.TurnLeft
            else Icons.Outlined.TurnRight
        is HapticCommand.Straight -> Icons.Outlined.Straight
        HapticCommand.Arrived -> Icons.Outlined.Flag
        is HapticCommand.ItinerarySync -> Icons.Outlined.Sync
        HapticCommand.Ping -> Icons.Outlined.Vibration
    }

@Composable
fun HapticCommand.describe(): String = when (this) {
    is HapticCommand.Turn -> stringResource(
        if (direction == TurnDirection.LEFT) R.string.haptic_turn_left else R.string.haptic_turn_right,
        formatDistance(distanceMeters),
    )
    is HapticCommand.Straight -> stringResource(R.string.haptic_straight, formatDistance(distanceMeters))
    HapticCommand.Arrived -> stringResource(R.string.haptic_arrived)
    is HapticCommand.ItinerarySync -> pluralStringResource(R.plurals.haptic_sync, stopCount, stopCount)
    HapticCommand.Ping -> stringResource(R.string.haptic_ping)
}
