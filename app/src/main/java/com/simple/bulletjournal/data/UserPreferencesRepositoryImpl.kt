package com.simple.bulletjournal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object Keys {
        val IS_AD_REMOVED = booleanPreferencesKey("is_ad_removed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            isAdRemoved = prefs[Keys.IS_AD_REMOVED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        )
    }

    override suspend fun setAdRemoved(isAdRemoved: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.IS_AD_REMOVED] = isAdRemoved }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = themeMode.name }
    }
}
