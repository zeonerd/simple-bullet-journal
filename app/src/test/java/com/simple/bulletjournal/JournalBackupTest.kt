package com.simple.bulletjournal

import com.simple.bulletjournal.data.InvalidBackupException
import com.simple.bulletjournal.data.JournalBackup
import com.simple.bulletjournal.data.Task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class JournalBackupTest {

    private val exportedAt = Instant.parse("2026-09-27T12:34:56Z")

    private val sample = listOf(
        Task(id = 11, date = "2026-09-18", content = "알바 검토", isMigrated = true, createdAt = 1000L),
        Task(id = 12, date = "2026-09-27", content = "따옴표 \"와 줄바꿈\n 포함", isCompleted = true, isPriority = true, orderIndex = 3, createdAt = 2000L)
    )

    private fun assertInvalid(json: String) {
        try {
            JournalBackup.decode(json)
            fail("InvalidBackupException expected")
        } catch (e: InvalidBackupException) {
            // expected
        }
    }

    @Test
    fun roundTrip_preservesAllFieldsExceptId() {
        val decoded = JournalBackup.decode(JournalBackup.encode(sample, exportedAt))

        assertEquals(sample.map { it.copy(id = 0) }, decoded.tasks)
        assertEquals(exportedAt, decoded.exportedAt)
    }

    @Test
    fun roundTrip_emptyJournal() {
        val decoded = JournalBackup.decode(JournalBackup.encode(emptyList(), exportedAt))
        assertTrue(decoded.tasks.isEmpty())
    }

    @Test
    fun decode_missingOptionalFields_useDefaults() {
        val decoded = JournalBackup.decode(
            """{"app":"todaynote","formatVersion":1,"tasks":[{"date":"2026-09-27","content":"최소 필드"}]}"""
        )
        assertEquals(listOf(Task(date = "2026-09-27", content = "최소 필드", createdAt = 0L)), decoded.tasks)
        assertNull(decoded.exportedAt)
    }

    @Test
    fun decode_rejectsOtherApps_futureVersions_andBrokenFiles() {
        assertInvalid("""{"app":"other-app","formatVersion":1,"tasks":[]}""")
        assertInvalid("""{"app":"todaynote","formatVersion":99,"tasks":[]}""")
        assertInvalid("""{"app":"todaynote","tasks":[]}""")
        assertInvalid("""{"app":"todaynote","formatVersion":1}""")
        assertInvalid("""{"app":"todaynote","formatVersion":1,"tasks":[{"date":"2026-09-27"}]}""")
        assertInvalid("""{"app":"todaynote","formatVersion":1,"tasks":[{"date":"27/09/2026","content":"x"}]}""")
        assertInvalid("not json at all")
        assertInvalid("")
    }

    @Test
    fun decode_acceptsLegacyBackupsMadeBeforeRename() {
        // 앱 이름 변경(불렛 저널 → 오늘노트) 전에 만든 백업 파일도 복원돼야 한다
        val legacy = """{"app":"simple-bullet-journal","formatVersion":1,"tasks":[{"date":"2026-09-27","content":"옛 백업"}]}"""
        assertEquals("옛 백업", JournalBackup.decode(legacy).tasks.single().content)
    }

    @Test
    fun encode_writesNewAppId() {
        assertTrue(JournalBackup.encode(emptyList(), exportedAt).contains("\"app\": \"todaynote\""))
    }

    @Test
    fun decode_oneBadTaskRejectsWholeFile() {
        // 일부만 복원되는 일이 없도록, 하나라도 잘못되면 파일 전체를 거부해야 한다
        val json = JournalBackup.encode(sample, exportedAt).replace("2026-09-27", "2026-13-45")
        assertInvalid(json)
    }

    @Test
    fun replaceAllTasks_replacesEverythingWithNewIds() = runTest {
        val repository = FakeTaskRepository()
        repository.insertTask(Task(date = "2026-09-26", content = "기존 기록"))

        val decoded = JournalBackup.decode(JournalBackup.encode(sample, exportedAt))
        repository.replaceAllTasks(decoded.tasks)

        val all = repository.getAllTasksOnce()
        assertEquals(listOf("알바 검토", "따옴표 \"와 줄바꿈\n 포함"), all.map { it.content })
        assertTrue(all.none { it.id == 0L })
    }
}
