package com.simple.bulletjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.data.TaskRepository
import com.simple.bulletjournal.widget.BulletJournalWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
            val manager = GlanceAppWidgetManager(application)
            val glanceIds = manager.getGlanceIds(BulletJournalWidget::class.java)
            glanceIds.forEach { glanceId ->
                BulletJournalWidget().update(application, glanceId)
            }
        } catch (_: Exception) {
            // Widget might not be placed yet
        }
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksByDate(date.format(formatter)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val yesterdayUncompletedTasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date ->
            repository.getTasksByDate(date.minusDays(1).format(formatter))
                .map { list -> list.filter { !it.isCompleted } }
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
        _selectedDate.value = LocalDate.now()
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
            val uncompleted = repository.getTasksByDateOnce(yesterday).filter { !it.isCompleted }
            if (uncompleted.isEmpty()) return@launch

            uncompleted.forEach { task ->
                repository.insertTask(
                    Task(
                        date = currentDate,
                        content = task.content
                    )
                )
            }
            updateWidget()
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
