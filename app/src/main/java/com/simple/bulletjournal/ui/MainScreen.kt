package com.simple.bulletjournal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.ui.ads.BannerAd
import com.simple.bulletjournal.ui.theme.LocalNotebookColors
import com.simple.bulletjournal.viewmodel.MigrationKind
import com.simple.bulletjournal.viewmodel.MigrationOffer
import com.simple.bulletjournal.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Notebook style constants ──────────────────────────────
private val LineHeight = 48.dp
private val MarginX = 36.dp

// ══════════════════════════════════════════════════════════
//  Main Screen
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    showAds: Boolean = false,
    onSettingsClick: () -> Unit = {},
    viewModel: TaskViewModel = hiltViewModel()
) {
    val colors = LocalNotebookColors.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val migrationOffer by viewModel.migrationOffer.collectAsState()
    var newTaskText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMigrationDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isToday = selectedDate == LocalDate.now()
    val notebookScrollState = rememberScrollState()
    var scrollToNewestTask by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // 연속 입력 중 키보드가 올라와 있어 공책 영역이 좁아지므로, 방금 추가한 할 일이 가려지지 않게 그 줄까지 스크롤한다.
    LaunchedEffect(tasks) {
        if (!scrollToNewestTask) return@LaunchedEffect
        scrollToNewestTask = false
        val newestIndex = tasks.indices.maxByOrNull { tasks[it].createdAt } ?: return@LaunchedEffect
        val lineHeightPx = with(density) { LineHeight.toPx() }
        val itemTop = (newestIndex * lineHeightPx).toInt()
        val itemBottom = ((newestIndex + 1) * lineHeightPx).toInt()
        val visibleTop = notebookScrollState.value
        val visibleBottom = visibleTop + notebookScrollState.viewportSize
        when {
            itemBottom > visibleBottom -> notebookScrollState.animateScrollTo(itemBottom - notebookScrollState.viewportSize)
            itemTop < visibleTop -> notebookScrollState.animateScrollTo(itemTop)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAppResumed()
    }

    Scaffold(
        containerColor = colors.paper
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // ── Top Bar (settings entry point) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "설정",
                        tint = colors.subtleText
                    )
                }
            }

            // ── Date Header ──
            DateHeader(
                date = selectedDate,
                isToday = isToday,
                onPreviousDay = viewModel::goToPreviousDay,
                onNextDay = viewModel::goToNextDay,
                onDateClick = { showDatePicker = true },
                onTodayClick = viewModel::goToToday
            )

            // ── Migration Banner (오늘: 지난 미완료 전체 / 내일: 오늘 남은 할 일) ──
            migrationOffer?.let { offer ->
                MigrationBanner(
                    offer = offer,
                    onMigrateClick = { showMigrationDialog = true }
                )
            }

            // ── Notebook Page ──
            NotebookPage(
                tasks = tasks,
                onToggle = viewModel::toggleTask,
                onTogglePriority = viewModel::togglePriority,
                onEdit = viewModel::editTask,
                onDelete = viewModel::deleteTask,
                scrollState = notebookScrollState,
                modifier = Modifier.weight(1f)
            )

            // ── Task Input Bar ──
            TaskInputBar(
                text = newTaskText,
                onTextChange = { newTaskText = it },
                // 불렛 저널의 "빠른 기록(Rapid Logging)": 추가 후에도 입력창 포커스를 유지해 바로 다음 할 일을 적게 한다.
                // 빈 칸에서 완료/추가를 누르면 입력을 끝낸 것으로 보고 키보드를 내린다.
                onAdd = {
                    if (newTaskText.isNotBlank()) {
                        viewModel.addTask(newTaskText)
                        newTaskText = ""
                        scrollToNewestTask = true
                    } else {
                        focusManager.clearFocus()
                    }
                }
            )

            // ── Banner Ad (광고 제거 미구매 시에만 노출) ──
            if (showAds) {
                BannerAd()
            }
        }
    }

    // ── Date Picker Dialog ──
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.selectDate(LocalDate.ofEpochDay(millis / 86400000L))
                        }
                        showDatePicker = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Migration Confirmation Dialog ──
    val offer = migrationOffer
    if (showMigrationDialog && offer != null) {
        val isPastToToday = offer.kind == MigrationKind.PAST_TO_TODAY
        AlertDialog(
            onDismissRequest = { showMigrationDialog = false },
            title = { Text(if (isPastToToday) "지난 할 일 가져오기" else "내일로 옮기기") },
            text = {
                Text(
                    if (isPastToToday) {
                        "지난 날짜에 완료하지 못한 할 일 ${offer.count}개를 오늘로 가져오시겠습니까?"
                    } else {
                        "오늘 완료하지 못한 할 일 ${offer.count}개를 내일로 옮기시겠습니까?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.migrateTasks()
                        showMigrationDialog = false
                    }
                ) { Text(if (isPastToToday) "가져오기" else "옮기기", color = colors.marginLine) }
            },
            dismissButton = {
                TextButton(onClick = { showMigrationDialog = false }) { Text("취소", color = colors.subtleText) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════
//  Notebook Page — ruled lines with tasks
// ══════════════════════════════════════════════════════════

@Composable
private fun NotebookPage(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onTogglePriority: (Task) -> Unit,
    onEdit: (Task, String) -> Unit,
    onDelete: (Task) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val visibleLines = (maxHeight / LineHeight).toInt() + 1
        val totalLines = maxOf(visibleLines, tasks.size + 1)
        val emptyLines = totalLines - tasks.size

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            tasks.forEach { task ->
                NotebookLine {
                    TaskOnLine(
                        task = task,
                        onToggle = { onToggle(task) },
                        onTogglePriority = { onTogglePriority(task) },
                        onEdit = { newContent -> onEdit(task, newContent) },
                        onDelete = { onDelete(task) }
                    )
                }
            }

            repeat(emptyLines) {
                NotebookLine()
            }
        }
    }
}

@Composable
private fun NotebookLine(
    content: @Composable (() -> Unit)? = null
) {
    val colors = LocalNotebookColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LineHeight)
            .drawBehind {
                // Horizontal ruled line at bottom
                drawLine(
                    color = colors.ruledLine,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
                // Vertical margin line
                val mx = MarginX.toPx()
                drawLine(
                    color = colors.marginLine,
                    start = Offset(mx, 0f),
                    end = Offset(mx, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        content?.invoke()
    }
}

// ══════════════════════════════════════════════════════════
//  Task Item on a Notebook Line
// ══════════════════════════════════════════════════════════

@Composable
private fun TaskOnLine(
    task: Task,
    onToggle: () -> Unit,
    onTogglePriority: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalNotebookColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editContent by remember(showEditDialog) { mutableStateOf(task.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MarginX + 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(36.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = colors.completed,
                uncheckedColor = colors.subtleText
            )
        )

        Text(
            text = task.content,
            modifier = Modifier
                .weight(1f)
                .clickable { showEditDialog = true },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                fontWeight = if (task.isPriority) FontWeight.SemiBold else FontWeight.Normal,
                color = if (task.isCompleted) colors.completed else colors.text
            )
        )

        IconButton(
            onClick = onTogglePriority,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (task.isPriority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (task.isPriority) "중요 해제" else "중요 표시",
                modifier = Modifier.size(18.dp),
                tint = if (task.isPriority) colors.priority else colors.subtleText.copy(alpha = 0.4f)
            )
        }

        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "삭제",
                modifier = Modifier.size(14.dp),
                tint = colors.subtleText.copy(alpha = 0.6f)
            )
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("할 일 수정") },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.marginLine,
                        unfocusedBorderColor = colors.ruledLine,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedContainerColor = colors.inputContainer,
                        unfocusedContainerColor = colors.inputContainer
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editContent.isNotBlank()) {
                            onEdit(editContent.trim())
                        }
                        showEditDialog = false
                    }
                ) { Text("수정", color = colors.marginLine) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소", color = colors.subtleText) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제") },
            text = { Text("'${task.content}'을(를) 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) { Text("삭제", color = colors.completed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소", color = colors.subtleText) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════
//  Migration Banner
// ══════════════════════════════════════════════════════════

@Composable
private fun MigrationBanner(
    offer: MigrationOffer,
    onMigrateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNotebookColors.current
    val isPastToToday = offer.kind == MigrationKind.PAST_TO_TODAY
    // 어제보다 오래된 할 일이 섞여 있으면 "언제부터 쌓였는지"를 한 줄 더 보여준다
    val sinceText = if (isPastToToday && offer.oldestDate < LocalDate.now().minusDays(1)) {
        offer.oldestDate.format(DateTimeFormatter.ofPattern("M월 d일부터", Locale.KOREAN))
    } else {
        null
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                color = colors.bannerBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPastToToday) "❭ 지난 미완료 할 일 ${offer.count}개" else "❭ 오늘 남은 할 일 ${offer.count}개",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.bannerText
            )
            if (sinceText != null) {
                Text(
                    text = sinceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.bannerText.copy(alpha = 0.7f)
                )
            }
        }
        TextButton(
            onClick = onMigrateClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
            Text(
                text = if (isPastToToday) "가져오기 ➔" else "내일로 ➔",
                style = MaterialTheme.typography.labelLarge,
                color = colors.marginLine
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Date Header
// ══════════════════════════════════════════════════════════

@Composable
private fun DateHeader(
    date: LocalDate,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit,
    onTodayClick: () -> Unit
) {
    val colors = LocalNotebookColors.current
    val displayFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "이전 날", tint = colors.text)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onDateClick)
            ) {
                Text(
                    text = "${date.year}년",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.subtleText
                )
                Text(
                    text = date.format(displayFormatter),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.text
                )
            }

            IconButton(onClick = onNextDay) {
                Icon(Icons.Default.ChevronRight, contentDescription = "다음 날", tint = colors.text)
            }
        }

        if (!isToday) {
            TextButton(
                onClick = onTodayClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("오늘로 돌아가기", color = colors.marginLine, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Task Input Bar
// ══════════════════════════════════════════════════════════

@Composable
private fun TaskInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    val colors = LocalNotebookColors.current
    HorizontalDivider(color = colors.ruledLine)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paper)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("할 일을 입력하세요", color = colors.subtleText) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.marginLine,
                unfocusedBorderColor = colors.ruledLine,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedContainerColor = colors.inputContainer,
                unfocusedContainerColor = colors.inputContainer
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledIconButton(
            onClick = onAdd,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.marginLine,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "추가")
        }
    }
}
