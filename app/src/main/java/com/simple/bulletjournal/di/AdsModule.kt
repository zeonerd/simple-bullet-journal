package com.simple.bulletjournal.di

import android.content.Context
import com.simple.bulletjournal.data.AdsConsentRepository
import com.simple.bulletjournal.data.AdsConsentRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    @Provides
    @Singleton
    fun provideAdsConsentRepository(
        @ApplicationContext context: Context
    ): AdsConsentRepository {
        return AdsConsentRepositoryImpl(context)
    }
}
