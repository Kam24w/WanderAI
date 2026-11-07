package com.vibra.wanderai.data.places

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.LocalizedText
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.repository.PlacesRepository

/**
 * Bundled catalogue of San Juan de Pasto. Coordinates are approximate (±50 m)
 * and costs are reference averages in COP. Swap this class for a Supabase
 * table-backed implementation without touching the domain or UI.
 */
class PastoPlacesRepository : PlacesRepository {

    override suspend fun getPlaces(): List<Place> = PLACES

    override suspend fun getPlace(id: String): Place? = PLACES_BY_ID[id]

    private companion object {
        val PLACES: List<Place> = listOf(
            // ── Heritage ────────────────────────────────────────────────────
            Place(
                id = "san-felipe",
                name = "Templo de San Felipe Neri",
                category = PlaceCategory.HERITAGE,
                location = GeoPoint(1.2147, -77.2822),
                rating = 4.8, averageCostCop = 0, visitMinutes = 25,
                summary = LocalizedText(
                    es = "Templo colonial con retablos tallados en madera y fachada de piedra.",
                    en = "Colonial church with carved wooden altarpieces and a stone façade.",
                ),
                history = LocalizedText(
                    es = "Templo colonial, siglo XVIII",
                    en = "Colonial temple, 18th century",
                ),
            ),
            Place(
                id = "catedral",
                name = "Catedral de Pasto",
                category = PlaceCategory.HERITAGE,
                location = GeoPoint(1.2155, -77.2790),
                rating = 4.7, averageCostCop = 0, visitMinutes = 25,
                summary = LocalizedText(
                    es = "Catedral de San Ignacio, sede de la diócesis de Pasto.",
                    en = "Saint Ignatius Cathedral, seat of the Diocese of Pasto.",
                ),
                history = LocalizedText(
                    es = "Templo jesuita, siglos XVII–XIX",
                    en = "Jesuit temple, 17th–19th century",
                ),
            ),
            Place(
                id = "san-juan",
                name = "Iglesia de San Juan Bautista",
                category = PlaceCategory.HERITAGE,
                location = GeoPoint(1.2141, -77.2801),
                rating = 4.8, averageCostCop = 0, visitMinutes = 20,
                summary = LocalizedText(
                    es = "La iglesia más antigua de la ciudad, junto a la Plaza de Nariño.",
                    en = "The city's oldest church, next to Plaza de Nariño.",
                ),
                history = LocalizedText(
                    es = "Fundación colonial, siglo XVI",
                    en = "Colonial foundation, 16th century",
                ),
            ),
            Place(
                id = "cristo-rey",
                name = "Iglesia de Cristo Rey",
                category = PlaceCategory.HERITAGE,
                location = GeoPoint(1.2126, -77.2826),
                rating = 4.7, averageCostCop = 0, visitMinutes = 20,
                summary = LocalizedText(
                    es = "Templo neogótico con vitrales y torres esbeltas.",
                    en = "Neo-Gothic church with stained glass and slender towers.",
                ),
                history = LocalizedText(
                    es = "Estilo neogótico, siglo XX",
                    en = "Neo-Gothic style, 20th century",
                ),
            ),
            // ── Museums ─────────────────────────────────────────────────────
            Place(
                id = "museo-oro",
                name = "Museo del Oro Nariño",
                category = PlaceCategory.MUSEUM,
                location = GeoPoint(1.2131, -77.2836),
                rating = 4.8, averageCostCop = 0, visitMinutes = 45,
                summary = LocalizedText(
                    es = "Orfebrería y cerámica de las culturas prehispánicas de Nariño.",
                    en = "Goldwork and pottery from Nariño's pre-Hispanic cultures.",
                ),
                history = LocalizedText(
                    es = "Colección del Banco de la República",
                    en = "Banco de la República collection",
                ),
            ),
            Place(
                id = "taminango",
                name = "Museo Taminango",
                category = PlaceCategory.MUSEUM,
                location = GeoPoint(1.2181, -77.2858),
                rating = 4.6, averageCostCop = 8_000, visitMinutes = 40,
                summary = LocalizedText(
                    es = "Casona de 1623 con artes y tradiciones populares de Nariño.",
                    en = "1623 house showcasing Nariño's folk arts and traditions.",
                ),
                history = LocalizedText(
                    es = "Casa colonial, siglo XVII",
                    en = "Colonial house, 17th century",
                ),
            ),
            Place(
                id = "museo-carnaval",
                name = "Casa del Carnaval",
                category = PlaceCategory.MUSEUM,
                location = GeoPoint(1.2109, -77.2779),
                rating = 4.5, averageCostCop = 5_000, visitMinutes = 35,
                summary = LocalizedText(
                    es = "Carrozas y máscaras del Carnaval de Negros y Blancos.",
                    en = "Floats and masks from the Black and White Carnival.",
                ),
                history = LocalizedText(
                    es = "Patrimonio Inmaterial de la Humanidad (UNESCO, 2009)",
                    en = "UNESCO Intangible Cultural Heritage (2009)",
                ),
            ),
            // ── Parks ───────────────────────────────────────────────────────
            Place(
                id = "plaza-narino",
                name = "Plaza de Nariño",
                category = PlaceCategory.PARK,
                location = GeoPoint(1.2137, -77.2811),
                rating = 4.6, averageCostCop = 0, visitMinutes = 20,
                summary = LocalizedText(
                    es = "Corazón histórico de Pasto, rodeado de edificios republicanos.",
                    en = "Pasto's historic heart, surrounded by republican-era buildings.",
                ),
            ),
            Place(
                id = "parque-infantil",
                name = "Parque Infantil",
                category = PlaceCategory.PARK,
                location = GeoPoint(1.2205, -77.2752),
                rating = 4.4, averageCostCop = 0, visitMinutes = 30,
                summary = LocalizedText(
                    es = "Zona verde amplia con senderos, ideal para caminar.",
                    en = "Wide green area with paths, ideal for walking.",
                ),
            ),
            Place(
                id = "parque-bolivar",
                name = "Parque Bolívar",
                category = PlaceCategory.PARK,
                location = GeoPoint(1.2062, -77.2731),
                rating = 4.3, averageCostCop = 0, visitMinutes = 25,
                summary = LocalizedText(
                    es = "Parque tradicional del sur con zonas de descanso.",
                    en = "Traditional southern park with rest areas.",
                ),
            ),
            // ── Viewpoints ──────────────────────────────────────────────────
            Place(
                id = "mirador-centenario",
                name = "Mirador Loma del Centenario",
                category = PlaceCategory.VIEWPOINT,
                location = GeoPoint(1.2208, -77.2884),
                rating = 4.7, averageCostCop = 0, visitMinutes = 30,
                summary = LocalizedText(
                    es = "Vista panorámica del valle de Atriz y el volcán Galeras.",
                    en = "Panoramic view of the Atriz valley and Galeras volcano.",
                ),
            ),
            Place(
                id = "mirador-rumipamba",
                name = "Mirador Rumipamba",
                category = PlaceCategory.VIEWPOINT,
                location = GeoPoint(1.2232, -77.2716),
                rating = 4.5, averageCostCop = 0, visitMinutes = 25,
                summary = LocalizedText(
                    es = "Atardeceres sobre la ciudad desde el oriente.",
                    en = "Sunsets over the city from the east.",
                ),
            ),
            Place(
                id = "mirador-galeras",
                name = "Mirador Vía al Galeras",
                category = PlaceCategory.VIEWPOINT,
                location = GeoPoint(1.2161, -77.2968),
                rating = 4.8, averageCostCop = 0, visitMinutes = 40,
                summary = LocalizedText(
                    es = "Punto alto con vista directa al volcán Galeras.",
                    en = "High point with a direct view of Galeras volcano.",
                ),
            ),
            // ── Restaurants ─────────────────────────────────────────────────
            Place(
                id = "pasto-cafe",
                name = "Pasto Café",
                category = PlaceCategory.RESTAURANT,
                location = GeoPoint(1.2150, -77.2830),
                rating = 4.9, averageCostCop = 18_000, visitMinutes = 35,
                summary = LocalizedText(
                    es = "Café de origen nariñense y repostería local.",
                    en = "Single-origin Nariño coffee and local pastries.",
                ),
            ),
            Place(
                id = "cuyeria",
                name = "Cuyería La Tradición",
                category = PlaceCategory.RESTAURANT,
                location = GeoPoint(1.2083, -77.2795),
                rating = 4.6, averageCostCop = 55_000, visitMinutes = 60,
                summary = LocalizedText(
                    es = "Cuy asado, plato emblemático de la cocina nariñense.",
                    en = "Roast guinea pig, the emblematic dish of Nariño cuisine.",
                ),
            ),
            Place(
                id = "sabores-narino",
                name = "Sabores de Nariño",
                category = PlaceCategory.RESTAURANT,
                location = GeoPoint(1.2122, -77.2799),
                rating = 4.5, averageCostCop = 28_000, visitMinutes = 50,
                summary = LocalizedText(
                    es = "Almuerzos típicos: locro, empanadas de añejo y hervidos.",
                    en = "Typical lunches: locro, añejo empanadas and hervidos.",
                ),
            ),
            Place(
                id = "cafe-galeras",
                name = "Café Galeras",
                category = PlaceCategory.RESTAURANT,
                location = GeoPoint(1.2170, -77.2805),
                rating = 4.4, averageCostCop = 15_000, visitMinutes = 30,
                summary = LocalizedText(
                    es = "Cafetería con terraza frente al centro histórico.",
                    en = "Café with a terrace facing the historic centre.",
                ),
            ),
        )

        val PLACES_BY_ID: Map<String, Place> = PLACES.associateBy(Place::id)
    }
}
