package com.uoa.ml.presentation.di

import com.uoa.core.database.daos.AIModelInputDao
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.ml.data.repository.AIModelInputRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModuleRepoProvider {

    @Provides
    @Singleton
    fun provideMlRepository(
        aiModelInputDao: AIModelInputDao
    ): AIModelInputRepository{
        return AIModelInputRepositoryImpl(aiModelInputDao)
    }

}
