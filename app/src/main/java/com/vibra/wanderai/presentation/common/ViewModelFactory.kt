package com.vibra.wanderai.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vibra.wanderai.core.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap content in CompositionLocalProvider(LocalAppContainer …)")
}

/** Creates a [ViewModel] scoped to the current NavBackStackEntry, injecting from [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> wanderViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
