package com.simple.bulletjournal.di

import android.content.Context
import com.simple.bulletjournal.data.BillingRepository
import com.simple.bulletjournal.data.BillingRepositoryImpl
import com.simple.bulletjournal.data.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingRepository(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository
    ): BillingRepository {
        return BillingRepositoryImpl(context, userPreferencesRepository)
    }
}
