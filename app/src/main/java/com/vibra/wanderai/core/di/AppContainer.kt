package com.vibra.wanderai.core.di

import android.content.Context
import com.vibra.wanderai.BuildConfig
import com.vibra.wanderai.core.DevEnvironment
import com.vibra.wanderai.core.network.SupabaseConfig
import com.vibra.wanderai.data.ai.GroqRecommendationRepository
import com.vibra.wanderai.data.auth.SupabaseAuthRepository
import com.vibra.wanderai.data.itinerary.DataStoreItineraryRepository
import com.vibra.wanderai.data.itinerary.SyncedItineraryRepository
import com.vibra.wanderai.data.location.AndroidLocationRepository
import com.vibra.wanderai.data.places.PastoPlacesRepository
import com.vibra.wanderai.data.settings.AndroidSettingsRepository
import com.vibra.wanderai.data.supabase.SupabaseAchievementsRepository
import com.vibra.wanderai.data.supabase.SupabaseProfileRepository
import com.vibra.wanderai.data.traveler.LocalTravelerRepository
import com.vibra.wanderai.data.vibra.BleVibraRepository
import com.vibra.wanderai.data.wear.PhoneGuidanceAnnouncer
import com.vibra.wanderai.data.wear.ResourceGuidanceTextProvider
import com.vibra.wanderai.data.wear.LocalDevBridge
import com.vibra.wanderai.data.wear.WearableDataLayerRepository
import com.vibra.wanderai.domain.repository.AchievementsRepository
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.domain.repository.ItineraryRepository
import com.vibra.wanderai.domain.repository.LocationRepository
import com.vibra.wanderai.domain.repository.PlacesRepository
import com.vibra.wanderai.domain.repository.ProfileRepository
import com.vibra.wanderai.domain.repository.SettingsRepository
import com.vibra.wanderai.domain.repository.TravelChatRepository
import com.vibra.wanderai.domain.repository.TravelerRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.domain.repository.WearDataLayerRepository
import com.vibra.wanderai.domain.usecase.CloudSyncCoordinator
import com.vibra.wanderai.domain.usecase.DeleteAccountUseCase
import com.vibra.wanderai.domain.usecase.GenerateItineraryUseCase
import com.vibra.wanderai.domain.usecase.IdentifyLandmarkUseCase
import com.vibra.wanderai.domain.usecase.NavigationGuidanceController
import com.vibra.wanderai.domain.usecase.ObserveTravelerLocationUseCase
import com.vibra.wanderai.domain.usecase.PlanSession
import com.vibra.wanderai.domain.usecase.SyncItineraryToWatchUseCase
import com.vibra.wanderai.domain.usecase.VibraLinkManager
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
        // DataStore local + espejo en la tabla saved_itineraries de Supabase.
        SyncedItineraryRepository(
            local = DataStoreItineraryRepository(appContext, placesRepository),
            client = SupabaseConfig.client,
            placesRepository = placesRepository,
        )
    }
    val profileRepository: ProfileRepository by lazy { SupabaseProfileRepository(SupabaseConfig.client) }
    val achievementsRepository: AchievementsRepository by lazy { SupabaseAchievementsRepository(SupabaseConfig.client) }
    val travelerRepository: TravelerRepository by lazy {
        LocalTravelerRepository(itineraryRepository, achievementsRepository)
    }
    val travelChatRepository: TravelChatRepository by lazy {
        GroqRecommendationRepository(BuildConfig.GROQ_API_KEY, placesRepository, BuildConfig.GROQ_MODEL)
    }
    val vibraRepository: VibraRepository by lazy { BleVibraRepository(appContext, applicationScope) }
    val wearDataLayerRepository: WearDataLayerRepository by lazy {
        WearableDataLayerRepository(
            context = appContext,
            scope = applicationScope,
            // Solo debug: puente ADB si Play services no empareja ("Got 0 accounts").
            devBridge = if (DevEnvironment.localBridgeEnabled) LocalDevBridge() else null,
        )
    }

    /** Wear OS guidance session; started from [com.vibra.wanderai.WanderAIApplication]. */
    val navigationGuidance by lazy {
        NavigationGuidanceController(
            wear = wearDataLayerRepository,
            activeItinerary = itineraryRepository.activeItinerary,
            hapticIntensity = vibraRepository.hapticIntensity,
            text = ResourceGuidanceTextProvider(appContext),
            announcer = PhoneGuidanceAnnouncer(appContext),
        )
    }

    /** Plan filters + map focus shared by Plan, AI chat and Explore. */
    val planSession by lazy { PlanSession() }

    /** Supabase profile / itineraries / achievements sync; started from WanderAIApplication. */
    val cloudSync by lazy {
        CloudSyncCoordinator(authRepository, profileRepository, itineraryRepository, achievementsRepository)
    }

    // Use cases
    val deleteAccount by lazy { DeleteAccountUseCase(authRepository, itineraryRepository, achievementsRepository) }
    val observeTravelerLocation by lazy { ObserveTravelerLocationUseCase(locationRepository) }
    val generateItinerary by lazy { GenerateItineraryUseCase(placesRepository) }
    val identifyLandmark by lazy { IdentifyLandmarkUseCase(placesRepository) }
    /** BLE scan with automatic fallback to Wear OS Data Layer nodes. */
    val vibraLinkManager by lazy { VibraLinkManager(vibraRepository, wearDataLayerRepository, applicationScope) }
    val syncItineraryToWatch by lazy { SyncItineraryToWatchUseCase(vibraRepository, navigationGuidance) }
}
