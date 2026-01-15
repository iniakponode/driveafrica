package com.uoa.ml.presentation.di

import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.repository.CauseRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.ml.domain.BatchInsertCauseUseCase
import com.uoa.ml.domain.BatchUpDateUnsafeBehaviourCauseUseCase
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.ml.domain.UpDateUnsafeBehaviourCauseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.TimeZone
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsecaseClassModules {

    @Provides
    @Singleton
    fun provideRunClassificationUseCase(
        onnxModelRunner: OnnxModelRunner,
        aiModelInputRepository: AIModelInputRepository,
        tripRepository: TripDataRepository,
        locationRepository: LocationRepository,
        rawSensorDataRepository: RawSensorDataRepository,
        tripFeatureStateDao: TripFeatureStateDao,
        @Named("TrainingTimeZone") trainingTimeZone: TimeZone
    ): RunClassificationUseCase {
        return RunClassificationUseCase(
            onnxModelRunner,
            aiModelInputRepository,
            tripRepository,
            locationRepository,
            rawSensorDataRepository,
            tripFeatureStateDao,
            trainingTimeZone

            )
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
