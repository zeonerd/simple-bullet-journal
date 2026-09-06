package com.simple.bulletjournal.data

import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getTasksByDate(date: String): Flow<List<Task>> {
        return taskDao.getTasksByDate(date)
    }

    override suspend fun getTasksByDateOnce(date: String): List<Task> {
        return taskDao.getTasksByDateOnce(date)
    }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}
