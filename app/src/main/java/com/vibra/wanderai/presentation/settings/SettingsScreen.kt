package com.vibra.wanderai.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.AppLanguage
import com.vibra.wanderai.domain.model.AuthState
import com.vibra.wanderai.domain.model.AuthUser
import com.vibra.wanderai.domain.model.ThemeMode
import com.vibra.wanderai.domain.repository.AuthRepository
import com.vibra.wanderai.domain.repository.SettingsRepository
import com.vibra.wanderai.domain.repository.VibraRepository
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.SectionCard
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val vibraRepository: VibraRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val user: StateFlow<AuthUser?> = authRepository.authState
        .map { (it as? AuthState.Authenticated)?.user }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Read on each composition: the activity is recreated when the language changes. */
    val language: AppLanguage get() = settingsRepository.currentLanguage()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) = settingsRepository.setLanguage(language)

    /** The root auth gate reacts to the session change and shows Login. */
    fun signOut() {
        vibraRepository.disconnect()
        viewModelScope.launch { authRepository.signOut() }
    }
}

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val viewModel = wanderViewModel { SettingsViewModel(it.settingsRepository, it.authRepository, it.vibraRepository) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        WanderTopBar(title = stringResource(R.string.settings_title), onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionCard(title = stringResource(R.string.appearance_title), icon = Icons.Outlined.Palette, iconTint = WanderTheme.colors.brandTeal) {
                ChoiceRow(
                    options = listOf(
                        Choice(ThemeMode.SYSTEM, stringResource(R.string.option_system), Icons.Outlined.PhoneAndroid),
                        Choice(ThemeMode.LIGHT, stringResource(R.string.light_theme), Icons.Outlined.LightMode),
                        Choice(ThemeMode.DARK, stringResource(R.string.dark_theme), Icons.Outlined.DarkMode),
                    ),
                    selected = themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            SectionCard(title = stringResource(R.string.language_title), icon = Icons.Outlined.Language, iconTint = MaterialTheme.colorScheme.primary) {
                ChoiceRow(
                    options = listOf(
                        Choice(AppLanguage.SYSTEM, stringResource(R.string.option_system), null),
                        Choice(AppLanguage.SPANISH, stringResource(R.string.spanish_language), null),
                        Choice(AppLanguage.ENGLISH, stringResource(R.string.english_language), null),
                    ),
                    selected = viewModel.language,
                    onSelect = viewModel::setLanguage,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = stringResource(R.string.settings_account), icon = Icons.Outlined.Person, iconTint = WanderTheme.colors.brandGold) {
                user?.let {
                    Text(it.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(it.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                }
                OutlinedButton(
                    onClick = viewModel::signOut,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.logout_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = stringResource(R.string.settings_version, com.vibra.wanderai.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private data class Choice<T>(val value: T, val label: String, val icon: ImageVector?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(options: List<Choice<T>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option.value == selected,
                onClick = { onSelect(option.value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = WanderTheme.colors.brandTeal,
                    activeContentColor = Color.White,
                ),
                icon = {
                    if (option.icon != null) {
                        Icon(option.icon, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                    } else {
                        SegmentedButtonDefaults.Icon(active = option.value == selected)
                    }
                },
            ) {
                Text(option.label, maxLines = 1)
            }
        }
    }
}
