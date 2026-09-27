package com.simple.bulletjournal.data

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class AdsConsentRepositoryImpl(
    private val context: Context
) : AdsConsentRepository {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    // requestConsentInfoUpdate 성공 콜백과 "이전 세션 동의" 경로가 동시에 초기화를 시도할 수 있으므로 한 번만 실행되게 막는다.
    private val mobileAdsInitStarted = AtomicBoolean(false)
    @Volatile private var mobileAdsInitialized = false

    private val _canShowAds = MutableStateFlow(false)
    override val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    override val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    override fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refreshPrivacyOptionsRequirement()
                    initializeMobileAdsIfAllowed()
                }
            },
            {
                // 동의 정보 갱신 실패(오프라인 등) — 이전 세션에 받은 동의가 있으면 그대로 사용
                refreshPrivacyOptionsRequirement()
                initializeMobileAdsIfAllowed()
            }
        )

        // Google 권장: 갱신을 기다리지 않고 이전 세션의 동의 상태로 먼저 광고 초기화를 시도해 첫 광고 노출을 앞당긴다.
        initializeMobileAdsIfAllowed()
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            refreshPrivacyOptionsRequirement()
            // 동의를 철회했다면 canRequestAds()가 false가 될 수 있으므로 광고 노출 여부도 다시 맞춘다
            _canShowAds.value = mobileAdsInitialized && consentInformation.canRequestAds()
            initializeMobileAdsIfAllowed()
        }
    }

    private fun refreshPrivacyOptionsRequirement() {
        _isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeMobileAdsIfAllowed() {
        if (!consentInformation.canRequestAds()) return
        if (!mobileAdsInitStarted.compareAndSet(false, true)) return
        MobileAds.initialize(context) {
            mobileAdsInitialized = true
            _canShowAds.value = consentInformation.canRequestAds()
        }
    }
}
