package com.simple.bulletjournal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY isPriority DESC, orderIndex ASC, createdAt ASC")
    fun getTasksByDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY isPriority DESC, orderIndex ASC, createdAt ASC")
    suspend fun getTasksByDateOnce(date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Insert
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    /**
     * 날짜가 [fromDate] 이상 [untilDate] 미만인 미완료·미이월 할 일. 날짜가 yyyy-MM-dd 문자열이라 문자열 비교가 곧 날짜 비교입니다.
     * 정렬은 날짜 순 → 같은 날짜 안에서는 화면 표시 순서(getTasksByDate)와 동일.
     */
    @Query(
        "SELECT * FROM tasks WHERE date >= :fromDate AND date < :untilDate AND isCompleted = 0 AND isMigrated = 0 " +
            "ORDER BY date ASC, isPriority DESC, orderIndex ASC, createdAt ASC"
    )
    fun getMigratableTasks(fromDate: String, untilDate: String): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks WHERE date >= :fromDate AND date < :untilDate AND isCompleted = 0 AND isMigrated = 0 " +
            "ORDER BY date ASC, isPriority DESC, orderIndex ASC, createdAt ASC"
    )
    suspend fun getMigratableTasksOnce(fromDate: String, untilDate: String): List<Task>

    /**
     * [fromDate] 이상 [untilDate] 미만 날짜의 미완료·미이월 할 일을 [toDate]로 복사하고 원본에 isMigrated를 표시합니다.
     * 하나의 트랜잭션으로 묶여 있어, 위젯의 "가져오기"를 연달아 누르거나 앱과 위젯에서 동시에 실행해도
     * 두 번째 실행은 이미 isMigrated된 원본만 보게 되어 중복 이월이 생기지 않습니다.
     * 한 번에 옮긴 항목들의 createdAt에 순번을 더해, 옮긴 뒤에도 원래 날짜·순서가 그대로 유지되게 합니다.
     * @return 이월한 할 일 개수
     */
    @Transaction
    suspend fun migrateUncompletedTasks(fromDate: String, untilDate: String, toDate: String): Int {
        val migratable = getMigratableTasksOnce(fromDate, untilDate)
        val baseCreatedAt = System.currentTimeMillis()
        migratable.forEachIndexed { index, task ->
            insertTask(
                Task(
                    date = toDate,
                    content = task.content,
                    isPriority = task.isPriority,
                    createdAt = baseCreatedAt + index
                )
            )
            updateTask(task.copy(isMigrated = true))
        }
        return migratable.size
    }
}
