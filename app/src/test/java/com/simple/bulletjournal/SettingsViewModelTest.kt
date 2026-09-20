package com.simple.bulletjournal

import android.app.Activity
import app.cash.turbine.test
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.viewmodel.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeUserPreferencesRepository
    private lateinit var fakeBillingRepository: FakeBillingRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
        fakeBillingRepository = FakeBillingRepository()
        viewModel = SettingsViewModel(
            repository = fakeRepository,
            billingRepository = fakeBillingRepository
        )
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
    fun purchaseAdRemoval_delegatesToBillingRepository() {
        viewModel.purchaseAdRemoval(TestActivity())

        assertEquals(1, fakeBillingRepository.purchaseAdRemovalCallCount)
    }

    @Test
    fun restorePurchases_delegatesToBillingRepository() {
        viewModel.restorePurchases()

        assertEquals(1, fakeBillingRepository.restorePurchasesCallCount)
    }

    private class TestActivity : Activity()
}
