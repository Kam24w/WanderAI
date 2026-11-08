package com.vibra.wanderai.data.google

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.PlaceCategory
import com.vibra.wanderai.domain.model.PlaceSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleProtocolsTest {

    @Test
    fun `decodes the reference polyline from Google docs`() {
        val points = PolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertEquals(GeoPoint(38.5, -120.2), points[0])
        assertEquals(40.7, points[1].latitude, 1e-9)
        assertEquals(-120.95, points[1].longitude, 1e-9)
        assertEquals(43.252, points[2].latitude, 1e-9)
        assertEquals(-126.453, points[2].longitude, 1e-9)
    }

    @Test
    fun `parses a real walking route through Pasto`() {
        // Response captured from the Routes API (Plaza de Nariño -> Catedral, 350 m).
        val path = GoogleRoutesProtocol.parsePath("""{"routes":[{"distanceMeters":350,"polyline":{"encodedPolyline":"malFl~tvMo@yB{CqIsBt@"}}]}""")
        assertEquals(4, path.size)
        assertEquals(1.21383, path.first().latitude, 1e-5)
        assertEquals(-77.28119, path.first().longitude, 1e-5)
        assertTrue(GoogleRoutesProtocol.parsePath("{}").isEmpty())
    }

    @Test
    fun `route request walks through intermediate stops`() {
        val stops = listOf(GeoPoint(1.0, -77.0), GeoPoint(1.1, -77.1), GeoPoint(1.2, -77.2))
        val body = Json.parseToJsonElement(GoogleRoutesProtocol.requestBody(stops)).jsonObject
        assertEquals("WALK", body["travelMode"]!!.jsonPrimitive.content)
        assertEquals(1, body["intermediates"]!!.jsonArray.size)
        val destination = body["destination"]!!.jsonObject["location"]!!.jsonObject["latLng"]!!.jsonObject
        assertEquals("1.2", destination["latitude"]!!.jsonPrimitive.content)

        val direct = Json.parseToJsonElement(GoogleRoutesProtocol.requestBody(stops.take(2))).jsonObject
        assertNull(direct["intermediates"])
    }

    @Test
    fun `nearby search body restricts to a circle around the traveler`() {
        val body = Json.parseToJsonElement(
            GooglePlacesProtocol.requestBody(GeoPoint(1.2136, -77.2811), 80_000, listOf("park"), "es"),
        ).jsonObject
        assertEquals("park", body["includedTypes"]!!.jsonArray.single().jsonPrimitive.content)
        assertEquals("es", body["languageCode"]!!.jsonPrimitive.content)
        val circle = body["locationRestriction"]!!.jsonObject["circle"]!!.jsonObject
        assertEquals(GooglePlacesProtocol.MAX_RADIUS_METERS.toDouble(), circle["radius"]!!.jsonPrimitive.content.toDouble(), 0.0)
    }

    @Test
    fun `maps nearby places to domain places`() {
        val body = """
            {"places":[
              {"id":"A1","displayName":{"text":"Parque Infantil"},"location":{"latitude":1.2149,"longitude":-77.2788},
               "rating":4.44,"primaryType":"park","types":["park","point_of_interest"]},
              {"id":"B2","displayName":{"text":"Restaurante La Merced"},"location":{"latitude":1.21,"longitude":-77.28},
               "rating":4.3,"priceLevel":"PRICE_LEVEL_MODERATE","primaryType":"steak_house","types":["steak_house","restaurant","food"],
               "editorialSummary":{"text":"Carnes a la parrilla"}},
              {"id":"C3","displayName":{"text":"Sin ubicación"}}
            ]}
        """.trimIndent()
        val places = GooglePlacesProtocol.parsePlaces(body)
        assertEquals(2, places.size)

        val park = places[0]
        assertEquals("g:A1", park.id)
        assertEquals(PlaceCategory.PARK, park.category)
        assertEquals(PlaceSource.GOOGLE, park.source)
        assertEquals(0, park.averageCostCop)
        assertEquals(4.4, park.rating, 1e-9)

        val restaurant = places[1]
        assertEquals(PlaceCategory.RESTAURANT, restaurant.category)
        assertEquals(35_000, restaurant.averageCostCop)
        assertTrue(restaurant.isCostEstimated)
        assertEquals("Carnes a la parrilla", restaurant.summary.es)
    }

    @Test
    fun `categories follow the most specific google type`() {
        assertEquals(PlaceCategory.MUSEUM, GooglePlacesProtocol.categoryFor(listOf("museum", "tourist_attraction")))
        assertEquals(PlaceCategory.HERITAGE, GooglePlacesProtocol.categoryFor(listOf("church")))
        assertEquals(PlaceCategory.VIEWPOINT, GooglePlacesProtocol.categoryFor(listOf("observation_deck")))
        assertEquals(PlaceCategory.PARK, GooglePlacesProtocol.categoryFor(listOf("state_park")))
        assertEquals(PlaceCategory.RESTAURANT, GooglePlacesProtocol.categoryFor(listOf("cafeteria")))
        assertEquals(PlaceCategory.HERITAGE, GooglePlacesProtocol.categoryFor(listOf("unknown_type")))
    }
}
