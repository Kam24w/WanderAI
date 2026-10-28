
package com.vibra.wanderai.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibra.wanderai.SupabaseConfig
import com.vibra.wanderai.data.preferences.LanguagePreferences
import com.vibra.wanderai.data.preferences.ThemePreferences
import com.vibra.wanderai.presentation.screens.home.HomeScreen
import com.vibra.wanderai.presentation.screens.login.LoginScreen
import com.vibra.wanderai.presentation.screens.register.RegisterScreen
import com.vibra.wanderai.presentation.screens.settings.SettingsScreen
import com.vibra.wanderai.presentation.screens.welcome.WelcomeScreen
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.*

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
    val sessionStatus by SupabaseConfig.client.auth.sessionStatus.collectAsState()

    if (sessionStatus is SessionStatus.Initializing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    val startDestination =
        if (sessionStatus is SessionStatus.Authenticated) {
            Routes.HOME
        } else {
            Routes.WELCOME
        }

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                },
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
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
                },
                onLogoutClick = {
                    coroutineScope.launch {
                        SupabaseConfig.client.auth.signOut()

                        navController.navigate(Routes.WELCOME) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }



            )
        }
    }
}
