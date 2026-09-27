package com.simple.bulletjournal.data

import kotlinx.coroutines.flow.Flow

/** "처음부터"를 뜻하는 이월 범위 하한. yyyy-MM-dd 문자열 비교에서 어떤 실제 날짜보다도 앞선다. */
const val EARLIEST_TASK_DATE = "0000-01-01"

interface TaskRepository {
    fun getTasksByDate(date: String): Flow<List<Task>>
    suspend fun getTasksByDateOnce(date: String): List<Task>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)

    /** [fromDate] 이상 [untilDate] 미만 날짜의 미완료·미이월 할 일 (이월 배너용) */
    fun getMigratableTasks(fromDate: String, untilDate: String): Flow<List<Task>>
    suspend fun getMigratableTasksOnce(fromDate: String, untilDate: String): List<Task>

    /** [fromDate] 이상 [untilDate] 미만 날짜의 미완료 할 일을 [toDate]로 원자적으로 이월합니다. 이월한 개수를 반환합니다. */
    suspend fun migrateUncompletedTasks(fromDate: String, untilDate: String, toDate: String): Int
}
