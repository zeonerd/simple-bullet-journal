package com.simple.bulletjournal.di

import android.content.Context
import com.simple.bulletjournal.data.BackupRepository
import com.simple.bulletjournal.data.BackupRepositoryImpl
import com.simple.bulletjournal.data.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        taskRepository: TaskRepository
    ): BackupRepository {
        return BackupRepositoryImpl(context, taskRepository)
    }
}
