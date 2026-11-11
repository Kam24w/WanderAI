package com.vibra.wanderai.data.itinerary

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private val Context.itineraryDataStore by preferencesDataStore(name = "wanderai_itineraries")

/**
 * Saved itineraries are stored as compact JSON (place ids + leg metrics) and
 * re-hydrated against the places catalogue, so catalogue updates propagate.
 */
class DataStoreItineraryRepository(
    private val context: Context,
    private val placesRepository: PlacesRepository,
) : ItineraryRepository {

    private val active = MutableStateFlow<Itinerary?>(null)
    override val activeItinerary: StateFlow<Itinerary?> = active.asStateFlow()

    override val savedItineraries: Flow<List<Itinerary>> =
        context.itineraryDataStore.data.map { prefs -> decode(prefs[SAVED_KEY]) }

    override fun setActive(itinerary: Itinerary?) {
        active.value = itinerary
    }

    override suspend fun save(itinerary: Itinerary) {
        context.itineraryDataStore.edit { prefs ->
            val current = JSONArray(prefs[SAVED_KEY] ?: "[]")
            val filtered = JSONArray()
            for (i in 0 until current.length()) {
                val item = current.getJSONObject(i)
                if (item.getString(F_ID) != itinerary.id) filtered.put(item)
            }
            filtered.put(encode(itinerary))
            prefs[SAVED_KEY] = filtered.toString()
        }
    }

    override suspend fun delete(itineraryId: String) {
        context.itineraryDataStore.edit { prefs ->
            val current = JSONArray(prefs[SAVED_KEY] ?: "[]")
            val filtered = JSONArray()
            for (i in 0 until current.length()) {
                val item = current.getJSONObject(i)
                if (item.getString(F_ID) != itineraryId) filtered.put(item)
            }
            prefs[SAVED_KEY] = filtered.toString()
        }
        if (active.value?.id == itineraryId) active.value = null
    }

    /** Device-only repository: nothing to pull (see [SyncedItineraryRepository]). */
    override suspend fun refreshFromCloud() = Unit

    override suspend fun clearLocal() {
        context.itineraryDataStore.edit { prefs ->
            prefs.remove(SAVED_KEY)
            prefs.remove(OWNER_KEY)
        }
        active.value = null
    }

    /** Supabase user the saved itineraries on this device belong to (null = never synced). */
    suspend fun ownerId(): String? = context.itineraryDataStore.data.first()[OWNER_KEY]

    suspend fun setOwner(userId: String) {
        context.itineraryDataStore.edit { it[OWNER_KEY] = userId }
    }

    suspend fun snapshot(): List<Itinerary> = savedItineraries.first()

    private fun encode(itinerary: Itinerary): JSONObject = JSONObject()
        .put(F_ID, itinerary.id)
        .put(F_CREATED, itinerary.createdAtMillis)
        .put(F_STOPS, JSONArray().apply {
            itinerary.stops.forEach { stop ->
                put(
                    JSONObject()
                        .put(F_PLACE, stop.place.id)
                        .put(F_WALK, stop.travelMinutes)
                        .put(F_DIST, stop.distanceMeters)
                )
            }
        })

    private suspend fun decode(raw: String?): List<Itinerary> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.getJSONObject(index)
                val stopsJson = json.getJSONArray(F_STOPS)
                val stops = (0 until stopsJson.length()).mapNotNull { s ->
                    val stop = stopsJson.getJSONObject(s)
                    placesRepository.getPlace(stop.getString(F_PLACE))?.let { place ->
                        ItineraryStop(place, stop.getInt(F_WALK), stop.getInt(F_DIST))
                    }
                }
                stops.takeIf { it.isNotEmpty() }?.let {
                    Itinerary(json.getString(F_ID), it, json.getLong(F_CREATED))
                }
            }.sortedByDescending(Itinerary::createdAtMillis)
        } catch (e: JSONException) {
            Log.w(TAG, "Discarding corrupt saved itineraries", e)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "ItineraryRepository"
        val SAVED_KEY = stringPreferencesKey("saved_itineraries")
        val OWNER_KEY = stringPreferencesKey("owner_user_id")
        const val F_ID = "id"
        const val F_CREATED = "createdAt"
        const val F_STOPS = "stops"
        const val F_PLACE = "placeId"
        const val F_WALK = "walkMin"
        const val F_DIST = "distM"
    }
}
