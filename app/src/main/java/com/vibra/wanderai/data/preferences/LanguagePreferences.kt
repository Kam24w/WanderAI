
package com.vibra.wanderai.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(
    name = "wanderai_language_preferences"
)

class LanguagePreferences(
    private val context: Context
) {
    private companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language_code")
    }

    val languageCode: Flow<String> =
        context.languageDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: "system"
        }

    suspend fun setLanguageCode(languageCode: String) {
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }
}
