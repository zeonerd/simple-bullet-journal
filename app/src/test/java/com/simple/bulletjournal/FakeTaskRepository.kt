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

    // 아래 세 메서드는 TaskDao의 getMigratableTasks / migrateUncompletedTasks와 동일한 규칙(범위·필터·정렬·createdAt 순번).
    // DAO 쪽 쿼리나 로직이 바뀌면 여기도 함께 맞출 것.
    private fun List<Task>.migratable(fromDate: String, untilDate: String): List<Task> =
        filter { it.date >= fromDate && it.date < untilDate && !it.isCompleted && !it.isMigrated }
            .sortedWith(
                compareBy<Task> { it.date }
                    .thenByDescending { it.isPriority }
                    .thenBy { it.orderIndex }
                    .thenBy { it.createdAt }
            )

    override fun getMigratableTasks(fromDate: String, untilDate: String): Flow<List<Task>> {
        return tasksFlow.map { it.migratable(fromDate, untilDate) }
    }

    override suspend fun getMigratableTasksOnce(fromDate: String, untilDate: String): List<Task> {
        return tasksFlow.value.migratable(fromDate, untilDate)
    }

    override suspend fun migrateUncompletedTasks(fromDate: String, untilDate: String, toDate: String): Int {
        val migratable = getMigratableTasksOnce(fromDate, untilDate)
        val baseCreatedAt = System.currentTimeMillis()
        migratable.forEachIndexed { index, task ->
            insertTask(
                Task(date = toDate, content = task.content, isPriority = task.isPriority, createdAt = baseCreatedAt + index)
            )
            updateTask(task.copy(isMigrated = true))
        }
        return migratable.size
    }
}
