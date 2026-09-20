package com.simple.bulletjournal.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.bulletjournal.data.BillingRepository
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.data.UserPreferences
import com.simple.bulletjournal.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

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
}
