package com.simple.bulletjournal

import app.cash.turbine.test
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.viewmodel.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
        viewModel = SettingsViewModel(repository = fakeRepository)
    }

    @Test
    fun defaultPreferences_areAdNotRemovedAndSystemTheme() = runTest {
        viewModel.userPreferences.test {
            val prefs = awaitItem()
            assertFalse(prefs.isAdRemoved)
            assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        }
    }

    @Test
    fun setThemeMode_updatesState() = runTest {
        viewModel.userPreferences.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            viewModel.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
        }
    }

    @Test
    fun setAdRemoved_updatesState() = runTest {
        viewModel.userPreferences.test {
            assertFalse(awaitItem().isAdRemoved)

            viewModel.setAdRemoved(true)
            assertTrue(awaitItem().isAdRemoved)
        }
    }
}
