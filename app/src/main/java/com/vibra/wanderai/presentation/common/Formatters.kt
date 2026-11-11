package com.vibra.wanderai.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.TravelMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val COLOMBIA = Locale.Builder().setLanguage("es").setRegion("CO").build()

/** "$80.000 COP" — Colombian grouping regardless of UI language. */
fun formatCop(amount: Int): String =
    "$" + NumberFormat.getIntegerInstance(COLOMBIA).format(amount) + " COP"

/** "$10k" / "$1.2M" for slider edges and compact cards. */
fun formatCopShort(amount: Int): String = when {
    amount == 0 -> "$0"
    amount >= 1_000_000 -> "$" + String.format(Locale.US, "%.1fM", amount / 1_000_000f)
    amount >= 1_000 -> "$" + (amount / 1_000) + "k"
    else -> "$$amount"
}

/** "2h 30min", "45min", "3h". */
fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> "${minutes}min"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h ${minutes.toString().padStart(2, '0')}min"
    }
}

/** "850 m" / "1.4 km". */
fun formatDistance(meters: Int): String =
    if (meters < 1_000) "$meters m" else String.format(Locale.US, "%.1f km", meters / 1_000f)

/** "1.2136°N, 77.2811°W" */
fun formatCoordinates(point: GeoPoint): String {
    val lat = String.format(Locale.US, "%.4f°%s", abs(point.latitude), if (point.latitude >= 0) "N" else "S")
    val lon = String.format(Locale.US, "%.4f°%s", abs(point.longitude), if (point.longitude >= 0) "E" else "W")
    return "$lat, $lon"
}

@Composable
@ReadOnlyComposable
fun currentLanguageCode(): String =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language ?: "es"

@Composable
@ReadOnlyComposable
fun LocalizedText.localized(): String = resolve(currentLanguageCode())

@Composable
fun walkingLabel(minutes: Int): String = travelTimeLabel(minutes, TravelMode.WALK)

/** "12 min a pie", "3 min en moto", "4 min en carro". */
@Composable
fun travelTimeLabel(minutes: Int, mode: TravelMode): String = stringResource(
    when (mode) {
        TravelMode.WALK -> R.string.common_walk_minutes
        TravelMode.MOTORCYCLE -> R.string.common_motorcycle_minutes
        TravelMode.CAR -> R.string.common_car_minutes
    },
    minutes,
)
