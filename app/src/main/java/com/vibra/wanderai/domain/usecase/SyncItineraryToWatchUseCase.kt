package com.vibra.wanderai.domain.usecase

import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.TurnDirection
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraError
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.repository.VibraRepository
import kotlin.math.abs

/**
 * Sends an itinerary to Vibra over every available transport and keeps
 * [VibraRepository.syncStatus] up to date for every screen that shows it:
 *  - Wear OS Data Layer: first cue via [NavigationGuidanceController] (the route
 *    DataItem is mirrored by the controller from the active itinerary);
 *  - BLE (or the on-phone simulator): itinerary header plus the first cue.
 *
 * The sync succeeds when at least one transport delivered it.
 */
class SyncItineraryToWatchUseCase(
    private val vibraRepository: VibraRepository,
    private val guidance: NavigationGuidanceController,
) {
    suspend operator fun invoke(itinerary: Itinerary, origin: GeoPoint): Result<Unit> {
        val first = itinerary.stops.firstOrNull()
            ?: return Result.failure(IllegalArgumentException("Empty itinerary"))

        val firstCue = firstInstruction(itinerary, origin)
        val bleConnected = vibraRepository.connectionState.value is VibraConnectionState.Connected

        vibraRepository.updateSyncStatus(WatchSyncStatus.Sending)
        val wearDelivered = guidance.startRoute(firstCue).isSuccess
        val bleResult = if (bleConnected) {
            vibraRepository
                .send(HapticCommand.ItinerarySync(itinerary.stops.size, first.distanceMeters))
                .mapCatching { vibraRepository.send(firstCue).getOrThrow() }
        } else {
            Result.failure(IllegalStateException("Vibra not connected over BLE"))
        }
        val result = if (wearDelivered) Result.success(Unit) else bleResult

        vibraRepository.updateSyncStatus(
            result.fold(
                onSuccess = {
                    WatchSyncStatus.Synced(
                        itineraryId = itinerary.id,
                        firstStopName = first.place.name,
                        firstStopMeters = first.distanceMeters,
                        firstStopWalkingMinutes = first.walkingMinutes,
                    )
                },
                onFailure = {
                    WatchSyncStatus.Failed(if (bleConnected) VibraError.WRITE_FAILED else VibraError.NOT_CONNECTED)
                },
            )
        )
        return result
    }

    /**
     * Without a compass heading we assume the traveler faces the first stop;
     * the cue announces the turn they will take there, derived from the bearing
     * change towards the second stop.
     */
    private fun firstInstruction(itinerary: Itinerary, origin: GeoPoint): HapticCommand {
        val stops = itinerary.stops
        val firstMeters = stops.first().distanceMeters
        if (stops.size < 2) return HapticCommand.Straight(firstMeters)

        val inBearing = origin.bearingTo(stops[0].place.location)
        val outBearing = stops[0].place.location.bearingTo(stops[1].place.location)
        val delta = ((outBearing - inBearing + 540.0) % 360.0) - 180.0
        return if (abs(delta) < STRAIGHT_TOLERANCE_DEGREES) {
            HapticCommand.Straight(firstMeters)
        } else {
            HapticCommand.Turn(
                direction = if (delta > 0) TurnDirection.RIGHT else TurnDirection.LEFT,
                distanceMeters = firstMeters,
            )
        }
    }

    private companion object {
        const val STRAIGHT_TOLERANCE_DEGREES = 25.0
    }
}
