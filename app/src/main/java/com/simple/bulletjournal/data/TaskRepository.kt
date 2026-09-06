package com.simple.bulletjournal.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksByDate(date: String): Flow<List<Task>>
    suspend fun getTasksByDateOnce(date: String): List<Task>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
}
