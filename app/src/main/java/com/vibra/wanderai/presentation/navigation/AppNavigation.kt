package com.vibra.wanderai.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibra.wanderai.R
import com.vibra.wanderai.core.network.SupabaseConfig
import com.vibra.wanderai.data.preferences.LanguagePreferences
import com.vibra.wanderai.data.preferences.ThemePreferences
import com.vibra.wanderai.presentation.screens.explore.ExploreScreen
import com.vibra.wanderai.presentation.screens.vibra.VibraIntegrationScreen
import com.vibra.wanderai.presentation.screens.home.HomeScreen
import com.vibra.wanderai.presentation.screens.login.LoginScreen
import com.vibra.wanderai.presentation.screens.profile.ProfileScreen
import com.vibra.wanderai.presentation.screens.register.RegisterScreen
import com.vibra.wanderai.presentation.screens.scanner.ScannerScreen
import com.vibra.wanderai.presentation.screens.settings.SettingsScreen
import com.vibra.wanderai.presentation.screens.welcome.WelcomeScreen
import com.vibra.wanderai.presentation.components.WanderLogo
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Route constants ───────────────────────────────────────────────────────────
private object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val VIBRA = "vibra"
}

private object TabRoutes {
    const val HOME = "tab_home"
    const val EXPLORE = "tab_explore"
    const val SCANNER = "tab_scanner"
    const val PROFILE = "tab_profile"
}

// ── Bottom nav items ──────────────────────────────────────────────────────────
private data class TabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val tabItems = listOf(
    TabItem(TabRoutes.HOME, R.string.tab_home, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    TabItem(TabRoutes.EXPLORE, R.string.tab_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    TabItem(TabRoutes.SCANNER, R.string.tab_scanner, Icons.Outlined.CameraAlt, Icons.Outlined.CameraAlt),
    TabItem(TabRoutes.PROFILE, R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

// ── Root Navigation ───────────────────────────────────────────────────────────
@Composable
fun AppNavigation(
    themePreferences: ThemePreferences,
    languagePreferences: LanguagePreferences,
    coroutineScope: CoroutineScope,
    themeMode: String,
    languageCode: String
) {
    val sessionStatus by SupabaseConfig.client.auth.sessionStatus.collectAsState()
    var timedOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3_000)
        timedOut = true
    }

    val isLoading = sessionStatus is SessionStatus.Initializing && !timedOut

    if (isLoading) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(NavyDeep, Color(0xFF0F2440)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WanderLogo(size = 80.dp, padding = 7.dp)
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = TealBrand, strokeWidth = 3.dp)
            }
        }
        return
    }

    val navController = rememberNavController()
    val startDestination =
        if (sessionStatus is SessionStatus.Authenticated) Routes.MAIN else Routes.WELCOME

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.MAIN) },
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.VIBRA) {
            VibraIntegrationScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.MAIN) {
            MainScreen(
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onVibraClick = { navController.navigate(Routes.VIBRA) },
                onLogoutClick = {
                    coroutineScope.launch {
                        SupabaseConfig.client.auth.signOut()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                languageCode = languageCode,
                onThemeChange = { mode -> coroutineScope.launch { themePreferences.setThemeMode(mode) } },
                onLanguageChange = { code -> coroutineScope.launch { languagePreferences.setLanguageCode(code) } },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    coroutineScope.launch {
                        SupabaseConfig.client.auth.signOut()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}

// ── MainScreen with bottom navigation ────────────────────────────────────────
@Composable
private fun MainScreen(
    onSettingsClick: () -> Unit,
    onVibraClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNavController.navigate(item.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelRes),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealBrand,
                            selectedTextColor = TealBrand,
                            indicatorColor = TealBrand.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = TabRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TabRoutes.HOME) {
                HomeScreen(onSettingsClick = onSettingsClick)
            }
            composable(TabRoutes.EXPLORE) {
                ExploreScreen(onVibraClick = onVibraClick)
            }
            composable(TabRoutes.SCANNER) {
                ScannerScreen()
            }
            composable(TabRoutes.PROFILE) {
                ProfileScreen(
                    onSettingsClick = onSettingsClick,
                    onLogoutClick = onLogoutClick,
                    onVibraClick = onVibraClick
                )
            }
        }
    }
}
