package com.vibra.wanderai.presentation.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.VibraConnectionState
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.presentation.chat.TravelChatScreen
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.explore.ExploreScreen
import com.vibra.wanderai.presentation.profile.ProfileScreen
import com.vibra.wanderai.presentation.recommendations.RecommendationsScreen
import com.vibra.wanderai.presentation.settings.SettingsScreen
import com.vibra.wanderai.presentation.vibra.VibraScreen
import com.vibra.wanderai.presentation.vision.VisionScreen
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object MainRoutes {
    const val PLAN = "plan"
    const val EXPLORE = "explore"
    const val VISION = "vision"
    const val VIBRA = "vibra"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
}

private data class Tab(
    val route: String,
    @param:StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val TABS = listOf(
    Tab(MainRoutes.PLAN, R.string.tab_plan, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    Tab(MainRoutes.EXPLORE, R.string.tab_explore, Icons.Filled.Map, Icons.Outlined.Map),
    Tab(MainRoutes.VISION, R.string.tab_vision, Icons.Outlined.CenterFocusStrong, Icons.Outlined.CenterFocusStrong),
    Tab(MainRoutes.VIBRA, R.string.tab_vibra, Icons.Filled.Watch, Icons.Outlined.Watch),
    Tab(MainRoutes.PROFILE, R.string.tab_profile, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
)

class MainViewModel(vibraRepository: VibraRepository) : ViewModel() {
    val isWatchConnected: StateFlow<Boolean> = vibraRepository.connectionState
        .map { it is VibraConnectionState.Connected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

/** Authenticated shell: bottom navigation + per-tab back stacks. */
@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val viewModel = wanderViewModel { MainViewModel(it.vibraRepository) }
    val watchConnected by viewModel.isWatchConnected.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route != MainRoutes.SETTINGS && currentDestination?.route != MainRoutes.CHAT

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                WanderBottomBar(
                    isSelected = { route -> currentDestination?.hierarchy?.any { it.route == route } == true },
                    watchConnected = watchConnected,
                    onSelect = navController::navigateToTab,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainRoutes.PLAN,
            modifier = Modifier.padding(padding),
        ) {
            composable(MainRoutes.PLAN) {
                RecommendationsScreen(
                    onSettingsClick = { navController.navigate(MainRoutes.SETTINGS) },
                    onShowOnMap = { navController.navigateToTab(MainRoutes.EXPLORE) },
                    onOpenChat = { navController.navigate(MainRoutes.CHAT) },
                )
            }
            composable(MainRoutes.CHAT) {
                TravelChatScreen(
                    onBack = { navController.popBackStack() },
                    onShowPlaceOnMap = { navController.navigateToTab(MainRoutes.EXPLORE) },
                )
            }
            composable(MainRoutes.EXPLORE) {
                ExploreScreen(
                    onOpenVibra = { navController.navigateToTab(MainRoutes.VIBRA) },
                    onGenerateItinerary = { navController.navigateToTab(MainRoutes.PLAN) },
                )
            }
            composable(MainRoutes.VISION) {
                VisionScreen(onOpenMap = { navController.navigateToTab(MainRoutes.EXPLORE) })
            }
            composable(MainRoutes.VIBRA) {
                VibraScreen(onGenerateItinerary = { navController.navigateToTab(MainRoutes.PLAN) })
            }
            composable(MainRoutes.PROFILE) {
                ProfileScreen(
                    onSettingsClick = { navController.navigate(MainRoutes.SETTINGS) },
                    onOpenItinerary = { navController.navigateToTab(MainRoutes.EXPLORE) },
                )
            }
            composable(MainRoutes.SETTINGS) {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun WanderBottomBar(
    isSelected: (String) -> Boolean,
    watchConnected: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = WanderTheme.colors
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.shadow(12.dp),
    ) {
        TABS.forEach { tab ->
            val selected = isSelected(tab.route)
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = {
                    when (tab.route) {
                        MainRoutes.VISION -> Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(6.dp, CircleShape)
                                .background(
                                    if (selected) colors.brandTeal else colors.brandNavy,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(tab.icon, contentDescription = label, tint = Color.White)
                        }
                        MainRoutes.VIBRA -> BadgedBox(
                            badge = { if (watchConnected) Badge(containerColor = colors.success) },
                        ) {
                            Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = label)
                        }
                        else -> Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = label)
                    }
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.brandTeal,
                    selectedTextColor = colors.brandTeal,
                    indicatorColor = if (tab.route == MainRoutes.VISION) Color.Transparent else colors.brandTeal.copy(alpha = 0.14f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
