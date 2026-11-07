package com.vibra.wanderai.presentation.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibra.wanderai.R
import com.vibra.wanderai.presentation.components.WanderTopBar
import com.vibra.wanderai.ui.theme.TealBrand

@Composable
fun SettingsScreen(
    themeMode: String,
    languageCode: String,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WanderTopBar(showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // ── Appearance ───────────────────────────────────────────────────
            SectionHeader(
                label = stringResource(R.string.appearance_title),
                icon = Icons.Outlined.LightMode
            )

            Spacer(Modifier.height(10.dp))

            SettingsOptionCard {
                SettingsOption(
                    label = stringResource(R.string.system_default),
                    icon = Icons.Outlined.PhoneAndroid,
                    selected = themeMode == "system",
                    onClick = { onThemeChange("system") }
                )
                SettingsOption(
                    label = stringResource(R.string.light_theme),
                    icon = Icons.Outlined.LightMode,
                    selected = themeMode == "light",
                    onClick = { onThemeChange("light") }
                )
                SettingsOption(
                    label = stringResource(R.string.dark_theme),
                    icon = Icons.Outlined.DarkMode,
                    selected = themeMode == "dark",
                    onClick = { onThemeChange("dark") },
                    showDivider = false
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Language ─────────────────────────────────────────────────────
            SectionHeader(
                label = stringResource(R.string.language_title),
                icon = Icons.Outlined.Language
            )

            Spacer(Modifier.height(10.dp))

            SettingsOptionCard {
                SettingsOption(
                    label = stringResource(R.string.system_language),
                    icon = Icons.Outlined.PhoneAndroid,
                    selected = languageCode == "system",
                    onClick = { onLanguageChange("system") }
                )
                SettingsOption(
                    label = stringResource(R.string.spanish_language),
                    icon = Icons.Outlined.Language,
                    selected = languageCode == "es",
                    onClick = { onLanguageChange("es") }
                )
                SettingsOption(
                    label = stringResource(R.string.english_language),
                    icon = Icons.Outlined.Language,
                    selected = languageCode == "en",
                    onClick = { onLanguageChange("en") },
                    showDivider = false
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Logout ───────────────────────────────────────────────────────
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    stringResource(R.string.logout_button),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TealBrand, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsOptionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (selected) TealBrand else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    null,
                    tint = TealBrand,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    if (showDivider) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
