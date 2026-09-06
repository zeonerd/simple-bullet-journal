package com.simple.bulletjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simple.bulletjournal.data.AppDatabase
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.data.TaskRepository
import com.simple.bulletjournal.data.TaskRepositoryImpl
import com.simple.bulletjournal.widget.BulletJournalWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository = TaskRepositoryImpl(
        AppDatabase.getInstance(application).taskDao()
    ),
    private val widgetUpdater: suspend () -> Unit = {
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
) : AndroidViewModel(application) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksByDate(date.format(formatter)) }
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

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
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
