package com.simple.bulletjournal

import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.data.UserPreferences
import com.simple.bulletjournal.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {

    private val preferencesFlow = MutableStateFlow(UserPreferences())

    override val userPreferences: Flow<UserPreferences> = preferencesFlow

    override suspend fun setAdRemoved(isAdRemoved: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(isAdRemoved = isAdRemoved)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferencesFlow.value = preferencesFlow.value.copy(themeMode = themeMode)
    }
}
