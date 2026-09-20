package com.simple.bulletjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }

    // TODO(Phase 2 - 인앱결제): Play Billing 구매 완료 콜백에서 호출하도록 교체.
    // 지금은 설정 화면 UI/상태 배선만 먼저 구현하기 위한 임시 직접 호출입니다.
    fun setAdRemoved(isAdRemoved: Boolean) {
        viewModelScope.launch {
            repository.setAdRemoved(isAdRemoved)
        }
    }
}
