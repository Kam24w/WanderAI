package com.vibra.wanderai.domain.repository

import com.vibra.wanderai.domain.model.AchievementType
import com.vibra.wanderai.domain.model.ActiveRouteState
import com.vibra.wanderai.domain.model.AppLanguage
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.model.ChatConversation
import com.vibra.wanderai.domain.model.ChatMessage
import com.vibra.wanderai.domain.model.ChatReply
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
import com.vibra.wanderai.domain.model.TravelChatContext
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

    /**
     * GDPR: deletes the Supabase user and, by cascade, every row that belongs to it
     * (profile, achievements, saved itineraries), then ends the local session.
     */
    suspend fun deleteAccount(): Result<Unit>
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

    /** Merges the itineraries stored in the cloud for the signed-in user into the local copy. */
    suspend fun refreshFromCloud()

    /** Removes every itinerary stored on this device (after deleting the account). */
    suspend fun clearLocal()
}

interface TravelerRepository {
    fun observeStats(): Flow<TravelerStats>
}

/** `profiles` table: one row per Supabase user. */
interface ProfileRepository {
    suspend fun upsertProfile(displayName: String): Result<Unit>
}

/** `achievements` catalogue + `user_achievements` (unlocks of the signed-in user). */
interface AchievementsRepository {
    val unlocked: StateFlow<Set<AchievementType>>
    suspend fun refresh(): Result<Unit>

    /** Records [types] as unlocked now; already unlocked ones are ignored. */
    suspend fun unlock(types: Set<AchievementType>): Result<Unit>
    fun clearLocal()
}

/** Per-user saved conversations of the travel chat (`chat_history`). */
interface ChatHistoryRepository {
    /** Conversations of the signed-in user, most recent first. */
    suspend fun list(): Result<List<ChatConversation>>

    /** Inserts or replaces [conversation] (same id). */
    suspend fun save(conversation: ChatConversation): Result<Unit>
    suspend fun delete(conversationId: String): Result<Unit>
    suspend fun deleteAll(): Result<Unit>
}

/** Real places around a point (Google Places API New, Nearby Search). */
interface NearbyPlacesRepository {
    suspend fun nearby(center: GeoPoint, radiusMeters: Int, languageCode: String): Result<List<Place>>
}

/** Walking path along real streets (Google Routes API). */
interface RoutesRepository {
    /** Path that visits [waypoints] in order; the first is the origin, the last the destination. */
    suspend fun walkingPath(waypoints: List<GeoPoint>): Result<List<GeoPoint>>
}

/** Conversational travel assistant (Groq) that recommends places from the catalogue. */
interface TravelChatRepository {
    /** [history] holds the previous messages, oldest first, without [message]. */
    suspend fun reply(history: List<ChatMessage>, message: String, context: TravelChatContext): Result<ChatReply>
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
