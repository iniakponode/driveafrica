package com.uoa.ml.presentation.di

import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.ml.utils.IncrementalAccelerationYMean
import com.uoa.ml.utils.IncrementalCourseStd
import com.uoa.ml.utils.IncrementalDayOfWeekMean
import com.uoa.ml.utils.IncrementalHourOfDayMean
import com.uoa.ml.utils.IncrementalSpeedStd
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.TimeZone
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object CalculatorModule {

    @Provides
    @Named("TrainingTimeZone")
    fun provideTrainingTimeZone(): TimeZone {
        return TimeZone.getTimeZone("UTC+1")
    }

    @Provides
    fun provideIncrementalCourseStd(
        minMaxValuesLoader: MinMaxValuesLoader,
        locationRepo: LocationRepository
    ): IncrementalCourseStd {
        return IncrementalCourseStd(minMaxValuesLoader, locationRepo)
    }

    @Provides
    fun provideIncrementalSpeedStd(
        minMaxValuesLoader: MinMaxValuesLoader
    ): IncrementalSpeedStd {
        return IncrementalSpeedStd(minMaxValuesLoader)
    }

    @Provides
    fun provideIncrementalAccelerationYMean(
        minMaxValuesLoader: MinMaxValuesLoader
    ): IncrementalAccelerationYMean {
        return IncrementalAccelerationYMean(minMaxValuesLoader)
    }

    @Provides
    fun provideIncrementalHourOfDayMean(
        @Named("TrainingTimeZone") trainingTimeZone: TimeZone,
        minMaxValuesLoader: MinMaxValuesLoader
    ): IncrementalHourOfDayMean {
        return IncrementalHourOfDayMean(trainingTimeZone, minMaxValuesLoader)
    }

    @Provides
    fun provideIncrementalDayOfWeekMean(
        @Named("TrainingTimeZone") trainingTimeZone: TimeZone,
        minMaxValuesLoader: MinMaxValuesLoader
    ): IncrementalDayOfWeekMean {
        return IncrementalDayOfWeekMean(trainingTimeZone, minMaxValuesLoader)
    }


}
