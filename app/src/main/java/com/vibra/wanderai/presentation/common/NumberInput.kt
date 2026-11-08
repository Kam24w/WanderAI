package com.vibra.wanderai.presentation.common

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Parsing of values typed with the numeric keyboard (Plan screen exact-value dialogs).
 * Pure functions so they are unit-tested on the JVM.
 */
object NumberInput {

    /** "80.000", "80000", "$80.000 COP", "80 000" → 80000. Null when there are no digits. */
    fun parseCop(text: String): Int? {
        val digits = text.filter(Char::isDigit)
        if (digits.isEmpty() || digits.length > 9) return null
        return digits.toInt()
    }

    /** "2.5", "2,5", "2.50 km", "3" → 2.5 / 2.5 / 2.5 / 3.0. Null for empty or malformed input. */
    fun parseKm(text: String): Float? {
        val cleaned = text.trim().lowercase(Locale.ROOT).removeSuffix("km").trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return null
        if (!cleaned.all { it.isDigit() || it == '.' }) return null
        return cleaned.toFloatOrNull()
    }

    /** Keeps only what the field accepts while typing (digits for COP). */
    fun sanitizeCop(text: String): String = text.filter(Char::isDigit).take(9)

    /** Digits and a single decimal separator (2 decimals max) for kilometres. */
    fun sanitizeKm(text: String): String {
        val out = StringBuilder()
        var separatorSeen = false
        var decimals = 0
        for (c in text) {
            when {
                c.isDigit() && !separatorSeen -> if (out.length < 3) out.append(c)
                c.isDigit() && decimals < 2 -> {
                    out.append(c)
                    decimals++
                }
                (c == '.' || c == ',') && !separatorSeen -> {
                    separatorSeen = true
                    out.append(if (out.isEmpty()) "0." else ".")
                }
            }
        }
        return out.toString()
    }

    /** "2.0 km" for whole tenths, "2.35 km" when the traveler typed hundredths. */
    fun formatKm(km: Float): String {
        val tenths = km * 10
        return if (abs(tenths - tenths.roundToInt()) < 0.001f) {
            String.format(Locale.US, "%.1f km", km)
        } else {
            String.format(Locale.US, "%.2f km", km)
        }
    }

    /** Plain editable form of [km] (no unit): "2.5", "2.35", "3". */
    fun editableKm(km: Float): String =
        formatKm(km).removeSuffix(" km").removeSuffix("0").removeSuffix(".").ifEmpty { "0" }
}
