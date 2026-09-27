package com.simple.bulletjournal.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import com.simple.bulletjournal.viewmodel.BackupMessage
import com.simple.bulletjournal.viewmodel.PendingRestore
import com.simple.bulletjournal.viewmodel.SettingsViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    val pendingRestore by viewModel.pendingRestore.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.billingNotices.collect { notice ->
            snackbarHostState.showSnackbar(notice.message())
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.backupMessages.collect { message ->
            snackbarHostState.showSnackbar(message.text())
        }
    }

    // 안드로이드 기본 파일 선택 화면(SAF)을 써서 저장소 권한 없이 사용자가 고른 위치(내 파일, Google Drive 등)에 읽고 쓴다
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onRestoreFileSelected) }

    Scaffold(
        containerColor = colors.paper,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
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

            // ── Data (Backup / Restore) Section ──
            SettingsSectionTitle("데이터")
            SettingsActionRow(
                title = "백업 파일 만들기",
                description = "모든 할 일 기록을 파일로 저장합니다",
                onClick = { exportLauncher.launch("bullet-journal-backup-${LocalDate.now()}.json") }
            )
            SettingsActionRow(
                title = "백업에서 복원",
                description = "백업 파일의 기록으로 전체를 바꿉니다",
                // 파일 관리자·드라이브마다 .json의 MIME 타입이 제각각이라 모든 파일을 보여주고, 내용 검증으로 걸러낸다
                onClick = { restoreLauncher.launch(arrayOf("*/*")) }
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

    pendingRestore?.let { pending ->
        RestoreConfirmDialog(
            pending = pending,
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore
        )
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
private fun RestoreConfirmDialog(
    pending: PendingRestore,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalNotebookColors.current
    val exportedText = pending.contents.exportedAt
        ?.atZone(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"))
        ?.let { " ($it 백업)" }
        .orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("백업에서 복원") },
        text = {
            Text(
                "현재 기록 ${pending.currentTaskCount}개를 모두 지우고, 백업 파일의 기록 ${pending.contents.tasks.size}개${exportedText}로 바꿉니다.\n\n" +
                    "이 작업은 되돌릴 수 없습니다. 필요하면 먼저 \"백업 파일 만들기\"로 현재 기록을 저장하세요."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("복원", color = colors.completed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = colors.subtleText) }
        }
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val colors = LocalNotebookColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, color = colors.text)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.subtleText
        )
    }
}

private fun BackupMessage.text(): String = when (this) {
    is BackupMessage.Exported -> "할 일 ${count}개를 백업 파일로 저장했습니다."
    BackupMessage.ExportFailed -> "백업 파일을 만들지 못했습니다. 다른 저장 위치를 선택해 주세요."
    is BackupMessage.InvalidFile -> "올바른 백업 파일이 아닙니다${reason?.let { " ($it)" }.orEmpty()}. 기존 기록은 그대로입니다."
    is BackupMessage.Restored -> "백업에서 할 일 ${count}개를 복원했습니다."
    BackupMessage.RestoreFailed -> "복원하지 못했습니다. 기존 기록은 그대로입니다."
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
