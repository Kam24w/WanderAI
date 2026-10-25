
package com.vibra.wanderai.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.vibra.wanderai.R

@Composable
fun SettingsScreen(
    themeMode: String,
    languageCode: String,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_description),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ThemeOption(
            label = stringResource(R.string.system_default),
            selected = themeMode == "system",
            onClick = { onThemeChange("system") }
        )

        ThemeOption(
            label = stringResource(R.string.light_theme),
            selected = themeMode == "light",
            onClick = { onThemeChange("light") }
        )

        ThemeOption(
            label = stringResource(R.string.dark_theme),
            selected = themeMode == "dark",
            onClick = { onThemeChange("dark") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        ThemeOption(
            label = stringResource(R.string.system_language),
            selected = languageCode == "system",
            onClick = { onLanguageChange("system") }
        )

        ThemeOption(
            label = stringResource(R.string.spanish_language),
            selected = languageCode == "es",
            onClick = { onLanguageChange("es") }
        )

        ThemeOption(
            label = stringResource(R.string.english_language),
            selected = languageCode == "en",
            onClick = { onLanguageChange("en") }
        )

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back_button))
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val buttonModifier = Modifier.fillMaxWidth()

    if (selected) {
        Button(
            onClick = onClick,
            modifier = buttonModifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier
        ) {
            Text(label)
        }
    }
}
