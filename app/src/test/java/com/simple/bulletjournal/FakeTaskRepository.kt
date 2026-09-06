package com.simple.bulletjournal

import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTaskRepository : TaskRepository {

    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun getTasksByDate(date: String): Flow<List<Task>> {
        return tasksFlow.map { list ->
            list.filter { it.date == date }.sortedWith(
                compareByDescending<Task> { it.isPriority }
                    .thenBy { it.orderIndex }
                    .thenBy { it.createdAt }
            )
        }
    }

    override suspend fun getTasksByDateOnce(date: String): List<Task> {
        return tasksFlow.value.filter { it.date == date }.sortedWith(
            compareByDescending<Task> { it.isPriority }
                .thenBy { it.orderIndex }
                .thenBy { it.createdAt }
        )
    }

    override suspend fun getTaskById(id: Long): Task? {
        return tasksFlow.value.find { it.id == id }
    }

    override suspend fun insertTask(task: Task) {
        val taskWithId = if (task.id == 0L) task.copy(id = nextId++) else task
        tasksFlow.value = tasksFlow.value + taskWithId
    }

    override suspend fun updateTask(task: Task) {
        tasksFlow.value = tasksFlow.value.map {
            if (it.id == task.id) task else it
        }
    }

    override suspend fun deleteTask(task: Task) {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == task.id }
    }
}
