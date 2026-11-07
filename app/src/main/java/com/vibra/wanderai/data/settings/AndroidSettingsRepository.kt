package com.vibra.wanderai.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibra.wanderai.domain.model.AppLanguage
import com.vibra.wanderai.domain.model.ThemeMode
import com.vibra.wanderai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "wanderai_preferences")

/**
 * Theme mode is persisted in DataStore.
 *
 * Language uses Android's native per-app locale API through AppCompat
 * (`LocaleManager` on API 33+, `AppLocalesMetadataHolderService` below),
 * so the choice also appears in the system "App languages" settings and the
 * activity is recreated with the right `values-xx` resources.
 */
class AndroidSettingsRepository(
    private val context: Context,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data
            .map { prefs ->
                when (prefs[THEME_KEY]) {
                    VALUE_LIGHT -> ThemeMode.LIGHT
                    VALUE_DARK -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }
            .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_KEY] = when (mode) {
                ThemeMode.SYSTEM -> VALUE_SYSTEM
                ThemeMode.LIGHT -> VALUE_LIGHT
                ThemeMode.DARK -> VALUE_DARK
            }
        }
    }

    override fun currentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return when (locales[0]?.language) {
            AppLanguage.ENGLISH.languageTag -> AppLanguage.ENGLISH
            AppLanguage.SPANISH.languageTag -> AppLanguage.SPANISH
            else -> AppLanguage.SYSTEM
        }
    }

    override fun setLanguage(language: AppLanguage) {
        val locales = language.languageTag
            ?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        const val VALUE_SYSTEM = "system"
        const val VALUE_LIGHT = "light"
        const val VALUE_DARK = "dark"
    }
}
