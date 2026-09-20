package com.simple.bulletjournal

import android.app.Activity
import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BulletJournalApp : Application() {

    lateinit var consentInformation: ConsentInformation
        private set

    override fun onCreate() {
        super.onCreate()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
    }

    fun requestConsentAndInitializeAds(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk(onReady)
                    }
                }
                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk(onReady)
                }
            },
            { }
        )
    }

    private var mobileAdsInitialized = false

    private fun initializeMobileAdsSdk(onReady: () -> Unit) {
        if (mobileAdsInitialized) {
            onReady()
            return
        }
        MobileAds.initialize(this) {
            mobileAdsInitialized = true
            onReady()
        }
    }
}
