package com.ilhanakd.aiphotovideoenhancer.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ilhanakd.aiphotovideoenhancer.domain.model.AppThemeOption
import com.ilhanakd.aiphotovideoenhancer.domain.model.LocaleOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ai_enhancer_settings")

class PreferenceRepository(private val context: Context) {

    private val languageKey = intPreferencesKey("language")
    private val themeKey = intPreferencesKey("theme")

    val language: Flow<LocaleOption> = context.dataStore.data.map { prefs ->
        when (prefs[languageKey] ?: 0) {
            1 -> LocaleOption.ENGLISH
            2 -> LocaleOption.TURKISH
            else -> LocaleOption.SYSTEM
        }
    }

    val theme: Flow<AppThemeOption> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey] ?: 0) {
            1 -> AppThemeOption.LIGHT
            2 -> AppThemeOption.DARK
            else -> AppThemeOption.SYSTEM
        }
    }

    suspend fun setLanguage(option: LocaleOption) {
        context.dataStore.edit { prefs: Preferences ->
            prefs[languageKey] = when (option) {
                LocaleOption.SYSTEM -> 0
                LocaleOption.ENGLISH -> 1
                LocaleOption.TURKISH -> 2
            }
        }
    }

    suspend fun setTheme(option: AppThemeOption) {
        context.dataStore.edit { prefs: Preferences ->
            prefs[themeKey] = when (option) {
                AppThemeOption.SYSTEM -> 0
                AppThemeOption.LIGHT -> 1
                AppThemeOption.DARK -> 2
            }
        }
    }
}
