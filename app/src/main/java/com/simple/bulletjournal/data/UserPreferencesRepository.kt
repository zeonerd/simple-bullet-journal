package com.simple.bulletjournal.data

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setAdRemoved(isAdRemoved: Boolean)
    suspend fun setThemeMode(themeMode: ThemeMode)
}
