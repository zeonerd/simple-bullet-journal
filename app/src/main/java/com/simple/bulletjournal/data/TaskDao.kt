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
     * [fromDate]의 미완료·미이월 할 일을 [toDate]로 복사하고 원본에 isMigrated를 표시합니다.
     * 하나의 트랜잭션으로 묶여 있어, 위젯의 "가져오기"를 연달아 누르거나 앱과 위젯에서 동시에 실행해도
     * 두 번째 실행은 이미 isMigrated된 원본만 보게 되어 중복 이월이 생기지 않습니다.
     * @return 이월한 할 일 개수
     */
    @Transaction
    suspend fun migrateUncompletedTasks(fromDate: String, toDate: String): Int {
        val uncompleted = getTasksByDateOnce(fromDate).filter { !it.isCompleted && !it.isMigrated }
        uncompleted.forEach { task ->
            insertTask(Task(date = toDate, content = task.content, isPriority = task.isPriority))
            updateTask(task.copy(isMigrated = true))
        }
        return uncompleted.size
    }
}
