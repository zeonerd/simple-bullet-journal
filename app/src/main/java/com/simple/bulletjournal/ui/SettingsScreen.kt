package com.simple.bulletjournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.simple.bulletjournal.data.ThemeMode
import com.simple.bulletjournal.ui.theme.LocalNotebookColors
import com.simple.bulletjournal.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalNotebookColors.current
    val preferences by viewModel.userPreferences.collectAsState()

    Scaffold(containerColor = colors.paper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = colors.text
                    )
                }
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text
                )
            }

            HorizontalDivider(color = colors.ruledLine)

            // ── Theme Section ──
            SettingsSectionTitle("테마")
            ThemeOption(
                label = "시스템 기본",
                selected = preferences.themeMode == ThemeMode.SYSTEM,
                onSelect = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            )
            ThemeOption(
                label = "라이트",
                selected = preferences.themeMode == ThemeMode.LIGHT,
                onSelect = { viewModel.setThemeMode(ThemeMode.LIGHT) }
            )
            ThemeOption(
                label = "다크",
                selected = preferences.themeMode == ThemeMode.DARK,
                onSelect = { viewModel.setThemeMode(ThemeMode.DARK) }
            )

            HorizontalDivider(color = colors.ruledLine)

            // ── Ad Removal Section ──
            SettingsSectionTitle("광고")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (preferences.isAdRemoved) "광고가 제거되었습니다" else "광고 없이 사용하기",
                    color = colors.text
                )
                if (!preferences.isAdRemoved) {
                    TextButton(onClick = { viewModel.setAdRemoved(true) }) {
                        Text("광고 제거", color = colors.marginLine)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    val colors = LocalNotebookColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = colors.subtleText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = LocalNotebookColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = colors.marginLine)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = colors.text)
    }
}
