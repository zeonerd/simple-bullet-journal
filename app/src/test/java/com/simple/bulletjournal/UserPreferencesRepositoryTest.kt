package com.simple.bulletjournal

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.data.UserPreferencesRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(): UserPreferencesRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_preferences.preferences_pb") }
        )
        return UserPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun defaultPreferences_areAdNotRemovedAndSystemTheme() = runTest {
        val repository = createRepository()

        repository.userPreferences.test {
            val prefs = awaitItem()
            assertFalse(prefs.isAdRemoved)
            assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        }
    }

    @Test
    fun setAdRemoved_persistsValue() = runTest {
        val repository = createRepository()

        repository.userPreferences.test {
            assertFalse(awaitItem().isAdRemoved)

            repository.setAdRemoved(true)
            assertTrue(awaitItem().isAdRemoved)
        }
    }

    @Test
    fun setThemeMode_persistsValue() = runTest {
        val repository = createRepository()

        repository.userPreferences.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
        }
    }
}
