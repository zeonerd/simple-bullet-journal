package com.simple.bulletjournal.data

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import com.simple.bulletjournal.widget.BulletJournalWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class BackupRepositoryImpl(
    private val context: Context,
    private val taskRepository: TaskRepository
) : BackupRepository {

    override suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val tasks = taskRepository.getAllTasksOnce()
        val json = JournalBackup.encode(tasks, Instant.now())
        // "wt": 같은 파일을 덮어쓸 때 이전 내용이 뒤에 남지 않도록 잘라낸다
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("백업 파일을 열 수 없습니다")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        tasks.size
    }

    override suspend fun readBackup(uri: Uri): BackupContents = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw InvalidBackupException("백업 파일을 열 수 없습니다")
        val bytes = input.use { it.readBytesLimited(MAX_BACKUP_BYTES) }
        JournalBackup.decode(bytes.toString(Charsets.UTF_8))
    }

    override suspend fun currentTaskCount(): Int = taskRepository.getAllTasksOnce().size

    override suspend fun restore(contents: BackupContents) {
        taskRepository.replaceAllTasks(contents.tasks)
        try {
            BulletJournalWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 사진 등 엉뚱한 큰 파일을 골랐을 때 메모리를 다 쓰지 않도록 상한을 둔다(할 일 수만 개도 수 MB 이내)
    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val read = read(chunk)
            if (read < 0) break
            buffer.write(chunk, 0, read)
            if (buffer.size() > limit) throw InvalidBackupException("백업 파일이 너무 큽니다")
        }
        return buffer.toByteArray()
    }

    companion object {
        private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
    }
}
