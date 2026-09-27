package com.simple.bulletjournal

import android.net.Uri
import com.simple.bulletjournal.data.BackupContents
import com.simple.bulletjournal.data.BackupRepository

class FakeBackupRepository : BackupRepository {
    override suspend fun exportTo(uri: Uri): Int = 0
    override suspend fun readBackup(uri: Uri): BackupContents = BackupContents(emptyList(), null)
    override suspend fun currentTaskCount(): Int = 0
    override suspend fun restore(contents: BackupContents) = Unit
}
