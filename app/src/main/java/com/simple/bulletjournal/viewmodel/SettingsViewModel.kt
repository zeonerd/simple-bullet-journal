package com.simple.bulletjournal.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.bulletjournal.data.AdsConsentRepository
import com.simple.bulletjournal.data.BillingNotice
import com.simple.bulletjournal.data.BillingRepository
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.data.UserPreferences
import com.simple.bulletjournal.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val billingRepository: BillingRepository,
    private val adsConsentRepository: AdsConsentRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    /** 결제/복원 결과 알림 — 설정 화면이 스낵바로 표시 */
    val billingNotices: Flow<BillingNotice> = billingRepository.notices

    val isPrivacyOptionsRequired: StateFlow<Boolean> = adsConsentRepository.isPrivacyOptionsRequired

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }

    fun purchaseAdRemoval(activity: Activity) {
        billingRepository.purchaseAdRemoval(activity)
    }

    fun restorePurchases() {
        billingRepository.restorePurchases()
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        adsConsentRepository.showPrivacyOptionsForm(activity)
    }
}
