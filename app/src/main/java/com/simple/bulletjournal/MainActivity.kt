package com.simple.bulletjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
