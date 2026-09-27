package com.simple.bulletjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simple.bulletjournal.data.EARLIEST_TASK_DATE
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.data.TaskRepository
import com.simple.bulletjournal.widget.BulletJournalWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    var widgetUpdater: suspend () -> Unit = {
        try {
            BulletJournalWidget().updateAll(application)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 테스트에서 "자정이 지난 상황"을 흉내낼 수 있도록 오늘 날짜 계산도 교체 가능하게 둔다(widgetUpdater와 같은 방식).
    var today: () -> LocalDate = { LocalDate.now() }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 이월 배너가 "오늘/내일"을 기준으로 달라지므로, 자정이 지나면 날짜를 보고 있던 페이지와 무관하게 다시 계산되도록 흐름으로 둔다.
    private val _today = MutableStateFlow(today())
    private val _selectedDate = MutableStateFlow(_today.value)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksByDate(date.format(formatter)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 현재 페이지에서 보여줄 이월 배너. 없으면 null.
     * - 오늘 페이지: 오늘 이전 모든 날짜의 미완료 할 일 → 오늘로 가져오기
     * - 내일 페이지: 오늘 남은 할 일 → 내일로 미리 옮기기
     * - 그 외 날짜: 배너 없음
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val migrationOffer: StateFlow<MigrationOffer?> = combine(_selectedDate, _today) { date, today -> date to today }
        .flatMapLatest { (date, today) ->
            val kind = migrationKindFor(date, today) ?: return@flatMapLatest flowOf(null)
            val (fromDate, untilDate) = migrationSourceRange(kind, today)
            repository.getMigratableTasks(fromDate, untilDate).map { tasks ->
                if (tasks.isEmpty()) {
                    null
                } else {
                    MigrationOffer(
                        kind = kind,
                        count = tasks.size,
                        oldestDate = LocalDate.parse(tasks.first().date, formatter)
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = today()
    }

    /**
     * 앱이 다시 화면에 나타날 때 호출. 앱을 켜둔 채 자정을 넘긴 경우, "오늘" 페이지를 보고 있던 사용자는
     * 새 날짜로 옮겨준다(어제 페이지에 할 일을 적는 실수 방지). 일부러 다른 날짜를 보고 있었다면 그대로 둔다.
     */
    fun onAppResumed() {
        val now = today()
        val previousToday = _today.value
        if (now == previousToday) return
        if (_selectedDate.value == previousToday) {
            _selectedDate.value = now
        }
        _today.value = now
    }

    fun addTask(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    date = _selectedDate.value.format(formatter),
                    content = content.trim()
                )
            )
            updateWidget()
        }
    }

    fun editTask(task: Task, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            repository.updateTask(task.copy(content = newContent.trim()))
            updateWidget()
        }
    }

    /** 현재 페이지의 이월 배너([migrationOffer])가 가리키는 할 일들을 현재 페이지 날짜로 옮깁니다. */
    fun migrateTasks() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val today = _today.value
            val kind = migrationKindFor(date, today) ?: return@launch
            val (fromDate, untilDate) = migrationSourceRange(kind, today)
            val migratedCount = repository.migrateUncompletedTasks(fromDate, untilDate, date.format(formatter))
            if (migratedCount > 0) updateWidget()
        }
    }

    private fun migrationKindFor(date: LocalDate, today: LocalDate): MigrationKind? = when (date) {
        today -> MigrationKind.PAST_TO_TODAY
        today.plusDays(1) -> MigrationKind.TODAY_TO_TOMORROW
        else -> null
    }

    private fun migrationSourceRange(kind: MigrationKind, today: LocalDate): Pair<String, String> = when (kind) {
        MigrationKind.PAST_TO_TODAY -> EARLIEST_TASK_DATE to today.format(formatter)
        MigrationKind.TODAY_TO_TOMORROW -> today.format(formatter) to today.plusDays(1).format(formatter)
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
            updateWidget()
        }
    }

    fun togglePriority(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPriority = !task.isPriority))
            updateWidget()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            updateWidget()
        }
    }

    private suspend fun updateWidget() {
        widgetUpdater()
    }
}

enum class MigrationKind { PAST_TO_TODAY, TODAY_TO_TOMORROW }

/** 이월 배너 표시 정보. [oldestDate]는 대상 중 가장 오래된 날짜(여러 날에 걸쳐 쌓였는지 안내용). */
data class MigrationOffer(
    val kind: MigrationKind,
    val count: Int,
    val oldestDate: LocalDate
)
