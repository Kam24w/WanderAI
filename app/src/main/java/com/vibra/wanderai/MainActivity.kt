package com.vibra.wanderai

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.core.network.SupabaseConfig
import com.vibra.wanderai.domain.model.ThemeMode
import com.vibra.wanderai.presentation.common.LocalAppContainer
import com.vibra.wanderai.presentation.root.WanderAIRoot
import com.vibra.wanderai.ui.theme.WanderAITheme
import io.github.jan.supabase.auth.handleDeeplinks

/**
 * AppCompatActivity (not ComponentActivity) so AppCompatDelegate's per-app
 * language API also works on Android 12 and below.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthDeeplink(intent)

        val container = (application as WanderAIApplication).container

        setContent {
            val themeMode by container.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                WanderAITheme(darkTheme = darkTheme) {
                    WanderAIRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeeplink(intent)
    }

    /** Completes e-mail confirmation links (wanderai://auth#access_token=…). */
    private fun handleAuthDeeplink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == SupabaseConfig.DEEPLINK_SCHEME && data.host == SupabaseConfig.DEEPLINK_HOST) {
            SupabaseConfig.client.handleDeeplinks(intent)
        }
    }
}
