package com.uoa.ml.presentation.di

import com.uoa.core.database.daos.AIModelInputDao
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.ml.data.repository.AIModelInputRepositoryImpl
import com.uoa.ml.utils.IncrementalAccelerationYMean
import com.uoa.ml.utils.IncrementalCourseStd
import com.uoa.ml.utils.IncrementalDayOfWeekMean
import com.uoa.ml.utils.IncrementalHourOfDayMean
import com.uoa.ml.utils.IncrementalSpeedStd
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
        aiModelInputDao: AIModelInputDao,
        incrementalHourOfDayMeanProvider: IncrementalHourOfDayMean,
        incrementalDayOfWeekMeanProvider: IncrementalDayOfWeekMean,
        incrementalSpeedStdProvider: IncrementalSpeedStd,
        incrementalAccelerationYMeanProvider: IncrementalAccelerationYMean,
        incrementalCourseStdProvider: IncrementalCourseStd,
        minMaxValuesLoader: MinMaxValuesLoader
    ): AIModelInputRepository{
        return AIModelInputRepositoryImpl(
            aiModelInputDao,
            incrementalHourOfDayMeanProvider,
            incrementalDayOfWeekMeanProvider,
            incrementalSpeedStdProvider,
            incrementalAccelerationYMeanProvider,
            incrementalCourseStdProvider,
            minMaxValuesLoader
        )
    }

}