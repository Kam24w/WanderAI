
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
import com.vibra.wanderai.data.preferences.LanguagePreferences
import com.vibra.wanderai.presentation.screens.login.LoginScreen
import com.vibra.wanderai.presentation.screens.register.RegisterScreen

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    themePreferences: ThemePreferences,
    languagePreferences: LanguagePreferences,
    coroutineScope: CoroutineScope,
    themeMode: String,
    languageCode: String
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
                },
                onLoginClick = {
                    navController.navigate(Routes.LOGIN)
                },
                onRegisterClick = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
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
                languageCode = languageCode,
                onThemeChange = { mode ->
                    coroutineScope.launch {
                        themePreferences.setThemeMode(mode)
                    }
                },
                onLanguageChange = { code ->
                    coroutineScope.launch {
                        languagePreferences.setLanguageCode(code)
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
