package com.vibra.wanderai.data.itinerary

import com.vibra.wanderai.data.supabase.SavedItineraryRow
import com.vibra.wanderai.data.supabase.SavedStopRow
import com.vibra.wanderai.data.supabase.SupabaseTables
import com.vibra.wanderai.data.supabase.currentUserId
import com.vibra.wanderai.data.supabase.requireUserId
import com.vibra.wanderai.data.supabase.runSupabase
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.ItineraryStop
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Local-first: DataStore stays the source the UI reads (works offline) and every
 * change is mirrored to the `saved_itineraries` table of the signed-in user.
 *
 * [refreshFromCloud] runs after sign-in: if the device copy belongs to another
 * account it is discarded first, then cloud and local are merged both ways.
 */
class SyncedItineraryRepository(
    private val local: DataStoreItineraryRepository,
    private val client: SupabaseClient,
    private val placesRepository: PlacesRepository,
) : ItineraryRepository by local {

    override suspend fun save(itinerary: Itinerary) {
        local.save(itinerary)
        if (client.currentUserId() != null) upload(listOf(itinerary))
    }

    override suspend fun delete(itineraryId: String) {
        local.delete(itineraryId)
        val userId = client.currentUserId() ?: return
        runSupabase("deleteItinerary") {
            client.from(SupabaseTables.SAVED_ITINERARIES).delete {
                filter {
                    eq("id", itineraryId)
                    eq("user_id", userId)
                }
            }
        }
    }

    override suspend fun refreshFromCloud() {
        val userId = client.currentUserId() ?: return
        if (local.ownerId() != userId) {
            local.clearLocal()
            local.setOwner(userId)
        }
        val remote = runSupabase("fetchItineraries") {
            client.from(SupabaseTables.SAVED_ITINERARIES)
                .select { filter { eq("user_id", userId) } }
                .decodeList<SavedItineraryRow>()
        }.getOrNull() ?: return

        val localItineraries = local.snapshot()
        val localIds = localItineraries.map(Itinerary::id).toSet()
        val remoteIds = remote.map(SavedItineraryRow::id).toSet()

        remote.filter { it.id !in localIds }.forEach { row -> row.toDomain()?.let { local.save(it) } }
        upload(localItineraries.filter { it.id !in remoteIds })
    }

    override suspend fun clearLocal() = local.clearLocal()

    private suspend fun upload(itineraries: List<Itinerary>) {
        if (itineraries.isEmpty()) return
        runSupabase("upsertItineraries") {
            val userId = client.requireUserId()
            client.from(SupabaseTables.SAVED_ITINERARIES).upsert(itineraries.map { it.toRow(userId) })
        }
    }

    private fun Itinerary.toRow(userId: String) = SavedItineraryRow(
        id = id,
        userId = userId,
        createdAtMs = createdAtMillis,
        totalMinutes = totalMinutes,
        totalDistanceM = totalDistanceMeters,
        estimatedCostCop = estimatedCostCop,
        stops = stops.map { SavedStopRow(it.place.id, it.travelMinutes, it.distanceMeters) },
    )

    private suspend fun SavedItineraryRow.toDomain(): Itinerary? {
        val domainStops = stops.mapNotNull { stop ->
            placesRepository.getPlace(stop.placeId)?.let { ItineraryStop(it, stop.walkMin, stop.distM) }
        }
        return domainStops.takeIf { it.isNotEmpty() }?.let { Itinerary(id, it, createdAtMs) }
    }
}
