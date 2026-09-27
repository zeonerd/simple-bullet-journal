package com.simple.bulletjournal

import android.app.Activity
import app.cash.turbine.test
import com.simple.bulletjournal.data.BillingNotice
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
    private lateinit var fakeBillingRepository: FakeBillingRepository
    private lateinit var fakeAdsConsentRepository: FakeAdsConsentRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
        fakeBillingRepository = FakeBillingRepository()
        fakeAdsConsentRepository = FakeAdsConsentRepository()
        viewModel = SettingsViewModel(
            repository = fakeRepository,
            billingRepository = fakeBillingRepository,
            adsConsentRepository = fakeAdsConsentRepository
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

    @Test
    fun billingNotices_areForwardedFromBillingRepository() = runTest {
        viewModel.billingNotices.test {
            fakeBillingRepository.noticesFlow.emit(BillingNotice.PRODUCT_UNAVAILABLE)
            assertEquals(BillingNotice.PRODUCT_UNAVAILABLE, awaitItem())
        }
    }

    @Test
    fun privacyOptions_reflectConsentRequirementAndDelegateForm() = runTest {
        viewModel.isPrivacyOptionsRequired.test {
            assertFalse(awaitItem())
            fakeAdsConsentRepository.isPrivacyOptionsRequiredFlow.value = true
            assertTrue(awaitItem())
        }

        viewModel.showPrivacyOptionsForm(TestActivity())
        assertEquals(1, fakeAdsConsentRepository.showPrivacyOptionsFormCallCount)
    }

    private class TestActivity : Activity()
}
