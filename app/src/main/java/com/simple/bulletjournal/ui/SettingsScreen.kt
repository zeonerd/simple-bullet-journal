package com.simple.bulletjournal.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.simple.bulletjournal.data.BillingNotice
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
    val isPrivacyOptionsRequired by viewModel.isPrivacyOptionsRequired.collectAsState()
    val activity = LocalContext.current as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    // 화면 전환을 NavHost 없이 MainActivity의 로컬 상태로 하므로, 시스템 뒤로가기를 직접 가로채지 않으면
    // 설정 화면에서 뒤로가기를 눌렀을 때 메인 화면이 아니라 앱 자체가 종료된다.
    BackHandler(onBack = onBack)

    LaunchedEffect(viewModel) {
        viewModel.billingNotices.collect { notice ->
            snackbarHostState.showSnackbar(notice.message())
        }
    }

    Scaffold(
        containerColor = colors.paper,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                    TextButton(onClick = { activity?.let(viewModel::purchaseAdRemoval) }) {
                        Text("광고 제거", color = colors.marginLine)
                    }
                }
            }

            if (!preferences.isAdRemoved) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.restorePurchases() }) {
                        Text("구매 복원", color = colors.subtleText)
                    }
                }
            }

            // ── 광고 개인정보 옵션 (EEA/영국 등 UMP가 요구하는 지역에서만 노출) ──
            if (isPrivacyOptionsRequired && !preferences.isAdRemoved) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activity?.let(viewModel::showPrivacyOptionsForm) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("광고 개인정보 설정", color = colors.text)
                }
            }
        }
    }
}

private fun BillingNotice.message(): String = when (this) {
    BillingNotice.PRODUCT_UNAVAILABLE -> "지금은 결제를 진행할 수 없습니다. 잠시 후 다시 시도해주세요."
    BillingNotice.PURCHASE_PENDING -> "결제가 처리 중입니다. 완료되면 광고가 자동으로 사라집니다."
    BillingNotice.PURCHASE_FAILED -> "결제를 완료하지 못했습니다. 잠시 후 다시 시도해주세요."
    BillingNotice.RESTORED -> "구매 내역을 복원했습니다."
    BillingNotice.NOTHING_TO_RESTORE -> "복원할 구매 내역이 없습니다."
    BillingNotice.RESTORE_FAILED -> "구매 내역을 확인하지 못했습니다. 네트워크 연결을 확인해주세요."
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
