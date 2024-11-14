package com.uoa.ml.presentation.di

import com.uoa.core.database.repository.CauseRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.core.mlclassifier.OnnxModelRunner
//import com.uoa.ml.Utils
import com.uoa.ml.UtilsNew
import com.uoa.ml.domain.BatchInsertCauseUseCase
import com.uoa.ml.domain.BatchUpDateUnsafeBehaviourCauseUseCase
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.ml.domain.UpDateUnsafeBehaviourCauseUseCase
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
object UsecaseClassModules {

    @Provides
    @Singleton
    fun provideRunClassificationUseCase(
        rawSensorDataRepository: RawSensorDataRepository,
        onnxModelRunner: OnnxModelRunner,
        incrementalCourseStdProvider: IncrementalCourseStd,
        incrementalSpeedStdProvider: IncrementalSpeedStd,
        incrementalAccelerationYMeanProvider: IncrementalAccelerationYMean,
        incrementalHourOfDayMeanProvider: IncrementalHourOfDayMean,
        minMaxValuesLoader: MinMaxValuesLoader,
        incrementalDayOfWeekMeanProvider: IncrementalDayOfWeekMean
    ): RunClassificationUseCase {
        return RunClassificationUseCase(rawSensorDataRepository,
            onnxModelRunner,
            incrementalCourseStdProvider,
            incrementalSpeedStdProvider,
            incrementalAccelerationYMeanProvider,
            incrementalHourOfDayMeanProvider,
            minMaxValuesLoader,
            incrementalDayOfWeekMeanProvider)
    }

    @Provides
    @Singleton
    fun provideBatchInsertCauseUseCase(
        causeRepository: CauseRepository,
        unsafeBehaviourRepository: UnsafeBehaviourRepository
    ): BatchInsertCauseUseCase {
        return BatchInsertCauseUseCase(causeRepository, unsafeBehaviourRepository)
    }

    @Provides
    @Singleton
    fun provideUpDateUnsafeBehaviourCauseUseCase(
        unsafeBehaviourRepository: UnsafeBehaviourRepository
    ): UpDateUnsafeBehaviourCauseUseCase {
        return UpDateUnsafeBehaviourCauseUseCase(unsafeBehaviourRepository)
    }

    @Provides
    @Singleton
    fun provideBatchUpDateUnsafeBehaviourCauseUseCase(
        unsafeBehaviourRepository: UnsafeBehaviourRepository
    ): BatchUpDateUnsafeBehaviourCauseUseCase {
        return BatchUpDateUnsafeBehaviourCauseUseCase(unsafeBehaviourRepository)
    }
}