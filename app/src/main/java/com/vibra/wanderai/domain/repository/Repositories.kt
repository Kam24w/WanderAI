package com.vibra.wanderai.domain.repository

import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.AppLanguage
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.NavigationAction
import com.vibra.wanderai.domain.model.NavigationEvent
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.SignUpResult
import com.vibra.wanderai.domain.model.ThemeMode
import com.vibra.wanderai.domain.model.TravelerStats
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.WatchSyncStatus
import com.vibra.wanderai.domain.model.WatchUserAction
import com.vibra.wanderai.domain.model.WearWatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(displayName: String, email: String, password: String): Result<SignUpResult>
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    suspend fun signOut()
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun currentLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}

interface LocationRepository {
    fun hasPermission(): Boolean

    /** Emits fixes while collected. Completes immediately when permission is missing. */
    fun observeLocation(): Flow<GeoPoint>
}

interface PlacesRepository {
    suspend fun getPlaces(): List<Place>
    suspend fun getPlace(id: String): Place?
}

interface ItineraryRepository {
    /** Itinerary currently shown on the map and synced to the watch. */
    val activeItinerary: StateFlow<Itinerary?>
    val savedItineraries: Flow<List<Itinerary>>
    fun setActive(itinerary: Itinerary?)
    suspend fun save(itinerary: Itinerary)
    suspend fun delete(itineraryId: String)
}

interface TravelerRepository {
    fun observeStats(): Flow<TravelerStats>
}

interface VibraRepository {
    val connectionState: StateFlow<VibraConnectionState>
    val discoveredDevices: StateFlow<List<VibraDevice>>
    val hapticIntensity: StateFlow<HapticIntensity>
    val syncStatus: StateFlow<WatchSyncStatus>
    val lastEvent: StateFlow<HapticEvent?>

    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun startScan()
    fun stopScan()
    fun connect(device: VibraDevice)
    fun disconnect()
    fun setHapticIntensity(intensity: HapticIntensity)
    suspend fun send(command: HapticCommand): Result<Unit>
    fun updateSyncStatus(status: WatchSyncStatus)
}

/** Wear OS Data Layer link with the Vibra watch app (see `WearProtocol`). */
interface WearDataLayerRepository {
    /** Watches reachable right now; refreshed while observed and after every send. */
    val connectedWatches: StateFlow<List<WearWatch>>

    /** Queries NodeClient now (also updates [connectedWatches]); empty when none or unavailable. */
    suspend fun refreshConnectedWatches(): List<WearWatch>

    /** Actions received from the watch on `/vibra/user_action`. */
    val userActions: Flow<WatchUserAction>

    /** Sends [event] to every connected watch; succeeds with the number of watches reached. */
    suspend fun sendNavigationEvent(event: NavigationEvent): Result<Int>

    /** Writes the persistent route DataItem; the Data Layer delivers it when a watch connects. */
    suspend fun updateActiveRoute(route: ActiveRouteState): Result<Unit>
}

/** Localized wording for navigation cues (implemented with Android resources). */
interface GuidanceTextProvider {
    fun instruction(action: NavigationAction, distanceMeters: Int): String
    fun distance(meters: Int): String
}

/** Phone-side feedback for actions triggered from the watch. */
interface GuidanceAnnouncer {
    /** Shows and speaks [instruction] again on the phone. */
    fun repeatInstruction(instruction: String)
    fun guidancePaused()
    fun guidanceResumed()
}
