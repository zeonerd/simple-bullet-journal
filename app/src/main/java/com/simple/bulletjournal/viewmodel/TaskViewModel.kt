package com.simple.bulletjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.flatMapLatest
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

    private var lastKnownToday: LocalDate = today()
    private val _selectedDate = MutableStateFlow(lastKnownToday)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksByDate(date.format(formatter)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val yesterdayUncompletedTasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date ->
            repository.getTasksByDate(date.minusDays(1).format(formatter))
                .map { list -> list.filter { !it.isCompleted && !it.isMigrated } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        if (now == lastKnownToday) return
        if (_selectedDate.value == lastKnownToday) {
            _selectedDate.value = now
        }
        lastKnownToday = now
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

    fun migrateYesterdayTasks() {
        viewModelScope.launch {
            val yesterday = _selectedDate.value.minusDays(1).format(formatter)
            val currentDate = _selectedDate.value.format(formatter)
            val migratedCount = repository.migrateUncompletedTasks(fromDate = yesterday, toDate = currentDate)
            if (migratedCount > 0) updateWidget()
        }
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
