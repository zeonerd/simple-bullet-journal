package com.simple.bulletjournal

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.ui.MainScreen
import com.simple.bulletjournal.ui.SettingsScreen
import com.simple.bulletjournal.ui.theme.BulletJournalTheme
import com.simple.bulletjournal.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var adsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        (application as BulletJournalApp).requestConsentAndInitializeAds(this) {
            adsReady = true
        }

        setContent {
            BulletJournalRoot(adsReady = adsReady)
        }
    }
}

@Composable
private fun BulletJournalRoot(
    adsReady: Boolean,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by settingsViewModel.userPreferences.collectAsState()
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var showSettings by remember { mutableStateOf(false) }
    val showAds = adsReady && !preferences.isAdRemoved

    // enableEdgeToEdge()는 앱 시작 시 OS 다크모드만 보고 상태바 아이콘 밝기를 한 번 정하고 끝나버려,
    // 사용자가 앱 내에서 테마를 OS와 다르게 선택하면(예: OS 다크 + 앱 라이트) 아이콘이 배경과 구분되지 않는다.
    // darkTheme이 바뀔 때마다 상태바 아이콘 밝기를 실제 배경에 맞춰 다시 계산해준다.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    BulletJournalTheme(darkTheme = darkTheme) {
        if (showSettings) {
            SettingsScreen(onBack = { showSettings = false })
        } else {
            MainScreen(
                showAds = showAds,
                onSettingsClick = { showSettings = true }
            )
        }
    }
}
