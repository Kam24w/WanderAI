package com.vibra.wanderai.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.vibra.wanderai.R

@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit
) {
    var destination by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.destination_hint)) },
            singleLine = true
        )

        Button(
            onClick = {
                // Destination search will be implemented later.
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = destination.isNotBlank()
        ) {
            Text(stringResource(R.string.explore_button))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_button))
        }
    }
}