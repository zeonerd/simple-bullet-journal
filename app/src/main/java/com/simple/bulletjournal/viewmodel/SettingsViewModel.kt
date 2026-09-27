package com.simple.bulletjournal.viewmodel

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.bulletjournal.data.AdsConsentRepository
import com.simple.bulletjournal.data.BackupContents
import com.simple.bulletjournal.data.BackupRepository
import com.simple.bulletjournal.data.BillingNotice
import com.simple.bulletjournal.data.BillingRepository
import com.simple.bulletjournal.data.InvalidBackupException
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.data.UserPreferences
import com.simple.bulletjournal.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val billingRepository: BillingRepository,
    private val adsConsentRepository: AdsConsentRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    /** 결제/복원 결과 알림 — 설정 화면이 스낵바로 표시 */
    val billingNotices: Flow<BillingNotice> = billingRepository.notices

    val isPrivacyOptionsRequired: StateFlow<Boolean> = adsConsentRepository.isPrivacyOptionsRequired

    private val _backupMessages = MutableSharedFlow<BackupMessage>(extraBufferCapacity = 1)
    /** 백업/복원 결과 알림 — 설정 화면이 스낵바로 표시 */
    val backupMessages: Flow<BackupMessage> = _backupMessages.asSharedFlow()

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    /** 검증을 통과해 사용자 확인을 기다리는 복원. null이 아니면 설정 화면이 확인창을 띄운다. */
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

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

    /** 파일 선택 화면에서 사용자가 고른 저장 위치에 백업 파일을 만든다. */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = backupRepository.exportTo(uri)
                _backupMessages.tryEmit(BackupMessage.Exported(count))
            } catch (e: Exception) {
                _backupMessages.tryEmit(BackupMessage.ExportFailed)
            }
        }
    }

    /** 복원할 파일을 고르면 먼저 읽고 검증만 한다. 통과하면 확인창을 띄우고(DB는 아직 그대로), 실패하면 이유를 알린다. */
    fun onRestoreFileSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val contents = backupRepository.readBackup(uri)
                _pendingRestore.value = PendingRestore(contents, backupRepository.currentTaskCount())
            } catch (e: InvalidBackupException) {
                _backupMessages.tryEmit(BackupMessage.InvalidFile(e.message))
            } catch (e: Exception) {
                _backupMessages.tryEmit(BackupMessage.InvalidFile(null))
            }
        }
    }

    fun confirmRestore() {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        viewModelScope.launch {
            try {
                backupRepository.restore(pending.contents)
                _backupMessages.tryEmit(BackupMessage.Restored(pending.contents.tasks.size))
            } catch (e: Exception) {
                _backupMessages.tryEmit(BackupMessage.RestoreFailed)
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }
}

data class PendingRestore(
    val contents: BackupContents,
    val currentTaskCount: Int
)

sealed interface BackupMessage {
    data class Exported(val count: Int) : BackupMessage
    data object ExportFailed : BackupMessage
    data class InvalidFile(val reason: String?) : BackupMessage
    data class Restored(val count: Int) : BackupMessage
    data object RestoreFailed : BackupMessage
}
