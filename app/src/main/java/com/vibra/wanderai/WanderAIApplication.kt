package com.vibra.wanderai

import android.app.Application
import com.vibra.wanderai.core.di.AppContainer

class WanderAIApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Listen to the Vibra Wear OS app and mirror the active route for the whole process lifetime.
        container.navigationGuidance.start(container.applicationScope)
        // Supabase: profile, saved itineraries and achievements of the signed-in traveler.
        container.cloudSync.start(container.applicationScope)
    }
}
