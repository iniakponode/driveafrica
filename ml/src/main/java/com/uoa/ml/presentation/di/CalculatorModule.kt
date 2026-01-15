package com.uoa.ml.presentation.di

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
    ): IncrementalCourseStd {
        return IncrementalCourseStd()
    }

    @Provides
    fun provideIncrementalSpeedStd(
    ): IncrementalSpeedStd {
        return IncrementalSpeedStd()
    }

    @Provides
    fun provideIncrementalAccelerationYMean(
    ): IncrementalAccelerationYMean {
        return IncrementalAccelerationYMean()
    }

    @Provides
    fun provideIncrementalHourOfDayMean(
        @Named("TrainingTimeZone") trainingTimeZone: TimeZone
    ): IncrementalHourOfDayMean {
        return IncrementalHourOfDayMean(trainingTimeZone)
    }

    @Provides
    fun provideIncrementalDayOfWeekMean(
        @Named("TrainingTimeZone") trainingTimeZone: TimeZone
    ): IncrementalDayOfWeekMean {
        return IncrementalDayOfWeekMean(trainingTimeZone)
    }


}
