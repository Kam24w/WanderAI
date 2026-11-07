package com.vibra.wanderai.core.di

import android.content.Context
import com.vibra.wanderai.core.network.SupabaseConfig
import com.vibra.wanderai.data.auth.SupabaseAuthRepository
import com.vibra.wanderai.data.itinerary.DataStoreItineraryRepository
import com.vibra.wanderai.data.location.AndroidLocationRepository
import com.vibra.wanderai.data.places.PastoPlacesRepository
import com.vibra.wanderai.data.settings.AndroidSettingsRepository
import com.vibra.wanderai.data.traveler.LocalTravelerRepository
import com.vibra.wanderai.data.vibra.BleVibraRepository
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.LocationRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import com.vibra.wanderai.domain.repository.SettingsRepository
import com.vibra.wanderai.domain.repository.TravelerRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.usecase.GenerateItineraryUseCase
import com.vibra.wanderai.domain.usecase.IdentifyLandmarkUseCase
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph (application-scoped singletons). Keeps the project
 * free of annotation processing while preserving constructor injection, so
 * swapping to Hilt later is mechanical.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Repositories
    val authRepository: AuthRepository by lazy { SupabaseAuthRepository(SupabaseConfig.client) }
    val settingsRepository: SettingsRepository by lazy { AndroidSettingsRepository(appContext) }
    val locationRepository: LocationRepository by lazy { AndroidLocationRepository(appContext) }
    val placesRepository: PlacesRepository by lazy { PastoPlacesRepository() }
    val itineraryRepository: ItineraryRepository by lazy {
        DataStoreItineraryRepository(appContext, placesRepository)
    }
    val travelerRepository: TravelerRepository by lazy { LocalTravelerRepository(itineraryRepository) }
    val vibraRepository: VibraRepository by lazy { BleVibraRepository(appContext, applicationScope) }

    // Use cases
    val observeTravelerLocation by lazy { ObserveTravelerLocationUseCase(locationRepository) }
    val generateItinerary by lazy { GenerateItineraryUseCase(placesRepository) }
    val identifyLandmark by lazy { IdentifyLandmarkUseCase(placesRepository) }
    val syncItineraryToWatch by lazy { SyncItineraryToWatchUseCase(vibraRepository) }
}
