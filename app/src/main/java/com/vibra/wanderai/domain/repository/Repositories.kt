package com.vibra.wanderai.domain.repository

import com.vibra.wanderai.domain.model.AppLanguage
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.model.GeoPoint
import com.vibra.wanderai.domain.model.HapticCommand
import com.vibra.wanderai.domain.model.HapticEvent
import com.vibra.wanderai.domain.model.HapticIntensity
import com.vibra.wanderai.domain.model.Itinerary
import com.vibra.wanderai.domain.model.Place
import com.vibra.wanderai.domain.model.SignUpResult
import com.vibra.wanderai.domain.model.ThemeMode
import com.vibra.wanderai.domain.model.TravelerStats
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.model.VibraDevice
import com.vibra.wanderai.domain.model.WatchSyncStatus
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
