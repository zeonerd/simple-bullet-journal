package com.simple.bulletjournal.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksByDate(date: String): Flow<List<Task>>
    suspend fun getTasksByDateOnce(date: String): List<Task>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)

    /** [fromDate]의 미완료 할 일을 [toDate]로 원자적으로 이월합니다. 이월한 개수를 반환합니다. */
    suspend fun migrateUncompletedTasks(fromDate: String, toDate: String): Int
}
