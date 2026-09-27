package com.simple.bulletjournal.data

import android.net.Uri

interface BackupRepository {
    /** 전체 기록을 [uri](사용자가 파일 선택 화면에서 고른 위치)에 백업 파일로 씁니다. 저장한 할 일 개수를 반환합니다. */
    suspend fun exportTo(uri: Uri): Int

    /** [uri]의 백업 파일을 읽고 검증만 합니다(DB는 건드리지 않음). 잘못된 파일이면 [InvalidBackupException]. */
    suspend fun readBackup(uri: Uri): BackupContents

    /** 현재 기록 개수 — 복원 확인창 안내용 */
    suspend fun currentTaskCount(): Int

    /** 기존 기록을 모두 [contents]의 기록으로 교체하고 위젯을 갱신합니다. */
    suspend fun restore(contents: BackupContents)
}
