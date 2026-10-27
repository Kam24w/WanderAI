
package com.vibra.wanderai

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.vibra.wanderai.data.preferences.LanguagePreferences
import com.vibra.wanderai.data.preferences.ThemePreferences
import com.vibra.wanderai.presentation.navigation.AppNavigation
import com.vibra.wanderai.ui.theme.WanderAITheme
import android.content.Intent

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val languagePreferences = LanguagePreferences(applicationContext)
        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val themeMode by themePreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = "system")

            val languageCode by languagePreferences.languageCode
                .collectAsStateWithLifecycle(initialValue = "system")

            val configuration = Configuration(
                LocalConfiguration.current
            )

            if (languageCode == "system") {
                val systemConfiguration = Configuration(
                    resources.configuration
                )
                configuration.setLocales(systemConfiguration.locales)
            } else {
                configuration.setLocale(
                    java.util.Locale.forLanguageTag(languageCode)
                )
            }

            val localizedContext = LocalContext.current
                .createConfigurationContext(configuration)

            val localizedResources = localizedContext.resources

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext,
                LocalResources provides localizedResources
            ) {
                val darkTheme = when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemInDarkTheme()
                }

                WanderAITheme(darkTheme = darkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            themePreferences = themePreferences,
                            languagePreferences = languagePreferences,
                            coroutineScope = lifecycleScope,
                            themeMode = themeMode,
                            languageCode = languageCode
                        )
                    }
                }
            }
        }
    }
}
