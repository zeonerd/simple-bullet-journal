package com.simple.bulletjournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isMigrated INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bullet_journal.db"
                )
                .addMigrations(MIGRATION_3_4)
                // v1/v2는 출시 전 개발 빌드에만 있던 스키마라 마이그레이션이 없다 — 이 두 버전에서만 초기화를 허용한다.
                // 그 외 버전에서 마이그레이션이 빠지면 사용자 기록을 조용히 지우는 대신 크래시로 드러나게 한다.
                // 스키마를 바꿀 때는 version을 올리고 Migration(N, N+1)을 추가한 뒤, 생성된 app/schemas/*.json을 함께 커밋할 것.
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
