package com.vibra.wanderai.domain.model

/**
 * Catalogue content (descriptions, history) available in both supported
 * languages. UI chrome lives in strings.xml; this is for data only.
 */
data class LocalizedText(
    val es: String,
    val en: String,
) {
    fun resolve(languageCode: String): String =
        if (languageCode.startsWith("en", ignoreCase = true)) en else es
}

enum class PlaceCategory {
    RESTAURANT,
    PARK,
    MUSEUM,
    VIEWPOINT,

    /** Churches and colonial monuments. Recognised by Vision, grouped with MUSEUM in filters. */
    HERITAGE;

    companion object {
        /** Categories the traveler can pick in the recommendations filter. */
        val selectable: List<PlaceCategory> = listOf(RESTAURANT, PARK, MUSEUM, VIEWPOINT)
    }
}

data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val location: GeoPoint,
    val rating: Double,
    val averageCostCop: Int,
    val visitMinutes: Int,
    val summary: LocalizedText,
    val history: LocalizedText? = null,
)
