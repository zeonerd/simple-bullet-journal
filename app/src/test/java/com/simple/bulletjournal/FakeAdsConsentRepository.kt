package com.simple.bulletjournal

import android.app.Activity
import com.simple.bulletjournal.data.AdsConsentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAdsConsentRepository : AdsConsentRepository {
    val canShowAdsFlow = MutableStateFlow(false)
    override val canShowAds: StateFlow<Boolean> = canShowAdsFlow

    val isPrivacyOptionsRequiredFlow = MutableStateFlow(false)
    override val isPrivacyOptionsRequired: StateFlow<Boolean> = isPrivacyOptionsRequiredFlow

    var showPrivacyOptionsFormCallCount = 0
        private set

    override fun gatherConsent(activity: Activity) = Unit

    override fun showPrivacyOptionsForm(activity: Activity) {
        showPrivacyOptionsFormCallCount++
    }
}
