package com.vibra.wanderai.core.network

import com.vibra.wanderai.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    /** Must match the intent-filter in AndroidManifest and the Supabase "Redirect URLs" list. */
    const val DEEPLINK_SCHEME = "wanderai"
    const val DEEPLINK_HOST = "auth"
    const val REDIRECT_URL = "$DEEPLINK_SCHEME://$DEEPLINK_HOST"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                scheme = DEEPLINK_SCHEME
                host = DEEPLINK_HOST
            }
            // Tablas profiles, achievements, user_achievements, saved_itineraries y RPC
            // delete_user_account (ver supabase/migrations).
            install(Postgrest)
        }
    }
}
