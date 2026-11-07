package com.vibra.wanderai.presentation.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.presentation.auth.AuthNavHost
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.WanderLogo
import com.vibra.wanderai.presentation.components.WanderWordmark
import com.vibra.wanderai.presentation.main.MainScaffold
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Single source of truth for the auth gate. If Supabase cannot restore a
 * session within [SESSION_RESTORE_TIMEOUT_MS] the user is sent to Login —
 * never into the app.
 */
class RootViewModel(authRepository: AuthRepository) : ViewModel() {

    private val restoreTimedOut = flow {
        emit(false)
        delay(SESSION_RESTORE_TIMEOUT_MS)
        emit(true)
    }

    val authState: StateFlow<AuthState> =
        combine(authRepository.authState.onStart { emit(AuthState.Loading) }, restoreTimedOut) { state, timedOut ->
            if (state is AuthState.Loading && timedOut) AuthState.Unauthenticated else state
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private companion object {
        const val SESSION_RESTORE_TIMEOUT_MS = 4_000L
    }
}

private enum class RootDestination { SPLASH, AUTH, MAIN }

@Composable
fun WanderAIRoot() {
    val viewModel = wanderViewModel { RootViewModel(it.authRepository) }
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val destination = when (authState) {
        AuthState.Loading -> RootDestination.SPLASH
        AuthState.Unauthenticated -> RootDestination.AUTH
        is AuthState.Authenticated -> RootDestination.MAIN
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "root",
    ) { target ->
        when (target) {
            RootDestination.SPLASH -> SplashScreen()
            RootDestination.AUTH -> AuthNavHost()
            RootDestination.MAIN -> MainScaffold()
        }
    }
}

@Composable
private fun SplashScreen() {
    val colors = WanderTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.heroStart, colors.heroEnd))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WanderLogo(size = 96.dp, padding = 8.dp, onDark = true)
            Spacer(Modifier.height(16.dp))
            WanderWordmark(onDark = true, fontSizeSp = 30)
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                color = colors.brandTeal,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
