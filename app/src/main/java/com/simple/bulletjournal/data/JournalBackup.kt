package com.simple.bulletjournal.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class InvalidBackupException(message: String) : Exception(message)

/** 백업 파일에서 읽어 들인 내용. [exportedAt]은 파일을 만든 시각(없거나 잘못된 경우 null). */
data class BackupContents(
    val tasks: List<Task>,
    val exportedAt: Instant?
)

/**
 * 백업 파일(JSON) 형식 정의와 변환.
 *
 * ```
 * { "app": "simple-bullet-journal", "formatVersion": 1, "exportedAt": "2026-09-27T12:00:00Z",
 *   "tasks": [ { "date": "2026-09-27", "content": "...", "isCompleted": false, "isPriority": false,
 *                "isMigrated": false, "orderIndex": 0, "createdAt": 1790000000000 } ] }
 * ```
 * - id는 담지 않습니다(복원 시 새로 발급).
 * - Task에 필드가 추가되면 [FORMAT_VERSION]을 올리고, [decode]가 이전 버전 파일도 읽을 수 있게(새 필드는 기본값) 유지할 것.
 */
object JournalBackup {
    const val APP_ID = "simple-bullet-journal"
    const val FORMAT_VERSION = 1

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun encode(tasks: List<Task>, exportedAt: Instant): String {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("date", task.date)
                    .put("content", task.content)
                    .put("isCompleted", task.isCompleted)
                    .put("isPriority", task.isPriority)
                    .put("isMigrated", task.isMigrated)
                    .put("orderIndex", task.orderIndex)
                    .put("createdAt", task.createdAt)
            )
        }
        return JSONObject()
            .put("app", APP_ID)
            .put("formatVersion", FORMAT_VERSION)
            .put("exportedAt", exportedAt.toString())
            .put("tasks", array)
            .toString(2)
    }

    /**
     * 파일 전체를 검사한 뒤에만 결과를 돌려줍니다. 하나라도 잘못되면 [InvalidBackupException] —
     * 호출 측은 이 예외가 나면 DB를 건드리지 않아야 합니다.
     */
    fun decode(json: String): BackupContents {
        try {
            val root = JSONObject(json)
            if (root.optString("app") != APP_ID) {
                throw InvalidBackupException("이 앱의 백업 파일이 아닙니다")
            }
            val version = root.optInt("formatVersion", -1)
            if (version !in 1..FORMAT_VERSION) {
                throw InvalidBackupException("지원하지 않는 백업 형식입니다(버전 $version) — 앱을 최신으로 업데이트해 주세요")
            }
            val array = root.getJSONArray("tasks")
            val tasks = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val date = item.getString("date")
                LocalDate.parse(date, dateFormatter) // 형식 검증
                Task(
                    date = date,
                    content = item.getString("content"),
                    isCompleted = item.optBoolean("isCompleted", false),
                    isPriority = item.optBoolean("isPriority", false),
                    isMigrated = item.optBoolean("isMigrated", false),
                    orderIndex = item.optInt("orderIndex", 0),
                    createdAt = item.optLong("createdAt", 0L)
                )
            }
            val exportedAt = root.optString("exportedAt").takeIf { it.isNotEmpty() }
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            return BackupContents(tasks, exportedAt)
        } catch (e: JSONException) {
            throw InvalidBackupException("백업 파일을 읽을 수 없습니다")
        } catch (e: DateTimeParseException) {
            throw InvalidBackupException("백업 파일의 날짜 형식이 올바르지 않습니다")
        }
    }
}
