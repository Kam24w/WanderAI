
package com.vibra.wanderai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibra.wanderai.data.preferences.ThemePreferences
import com.vibra.wanderai.presentation.screens.home.HomeScreen
import com.vibra.wanderai.presentation.screens.settings.SettingsScreen
import com.vibra.wanderai.presentation.screens.welcome.WelcomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    themePreferences: ThemePreferences,
    coroutineScope: CoroutineScope,
    themeMode: String
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Routes.HOME)
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeChange = { mode ->
                    coroutineScope.launch {
                        themePreferences.setThemeMode(mode)
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
