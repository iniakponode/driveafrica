package com.uoa.dbda.presentation.di

import com.uoa.core.database.daos.CauseDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.repository.CauseRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.dbda.domain.usecase.GetUnsafeBehavioursBetweenDatesUseCase
import com.uoa.dbda.domain.usecase.GetUnsafeBehavioursBySyncStatusUseCase
import com.uoa.dbda.domain.usecase.GetUnsafeBehavioursByTripIdUseCase
import com.uoa.dbda.domain.usecase.InsertUnsafeBehaviourUseCase
import com.uoa.dbda.domain.usecase.UpdateUnsafeBehaviourUseCase
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import com.uoa.dbda.domain.usecase.AnalyzeUnsafeBehaviorUseCase
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByDateUseCase
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByTripIdUseCase
import com.uoa.dbda.domain.usecase.analyser.UnsafeBehaviorAnalyser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DBDAModules {
    // add UnsafeBehaviourReportRepository provider here
   @Provides
    fun provideUnsafeBehaviourRepository(unsafeBehaviourDao: UnsafeBehaviourDao, rawSensorDataDao: RawSensorDataDao): UnsafeBehaviourRepository {
        return UnsafeBehaviourRepositoryImpl(unsafeBehaviourDao, rawSensorDataDao)
    }

    @Provides
    fun providesCauseRepository(causeDao: CauseDao): CauseRepository {
        return com.uoa.dbda.repository.CauseRepositoryImpl(causeDao)
    }

//    provide UnsafeBehaviourAnalyser provider here
    @Provides
    fun provideUnsafeBehaviourAnalyser(): UnsafeBehaviorAnalyser {
        return UnsafeBehaviorAnalyser()
    }

    // provide AnalyseUnsafeBehaviourUseCase provider here
    @Provides
    fun provideAnalyzeUnsafeBehaviourUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl, unsafeBehaviourAnalyser: UnsafeBehaviorAnalyser): AnalyzeUnsafeBehaviorUseCase {
        return AnalyzeUnsafeBehaviorUseCase(unsafeBehaviourAnalyser,unsafeBehaviourRepositoryImpl)
    }

//    Provides GetUnsafeBehavioursBetweenDatesUseCase
    @Provides
    fun provideGetUnsafeBehavioursBetweenDatesUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): GetUnsafeBehavioursBetweenDatesUseCase {
        return GetUnsafeBehavioursBetweenDatesUseCase(unsafeBehaviourRepositoryImpl)
    }

//    Provides GetUnsafeBehavioursBySyncStatusUseCase
    @Provides
    fun provideGetUnsafeBehavioursBySyncStatusUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): GetUnsafeBehavioursBySyncStatusUseCase {
        return GetUnsafeBehavioursBySyncStatusUseCase(unsafeBehaviourRepositoryImpl)
    }

//    Provides GetUnsafeBehavioursByTripIdUseCase
    @Provides
    fun provideGetUnsafeBehavioursByTripIdUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): GetUnsafeBehavioursByTripIdUseCase {
        return GetUnsafeBehavioursByTripIdUseCase(unsafeBehaviourRepositoryImpl)
    }

//    Provides InsertUnsafeBehaviourUseCase
    @Provides
    fun provideInsertUnsafeBehaviourUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): InsertUnsafeBehaviourUseCase {
        return InsertUnsafeBehaviourUseCase(unsafeBehaviourRepositoryImpl)
    }

//    provides UpdateUnsafeBehaviourUseCase
    @Provides
    fun provideUpdateUnsafeBehaviourUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): UpdateUnsafeBehaviourUseCase {
        return UpdateUnsafeBehaviourUseCase(unsafeBehaviourRepositoryImpl)
    }

    @Provides
    fun provideFetchRawSensorDataByDateUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): FetchRawSensorDataByDateUseCase {
        return FetchRawSensorDataByDateUseCase(unsafeBehaviourRepositoryImpl)
    }

    @Provides
    fun provideFetchRawSensorDataByTripIdUseCase(unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl): FetchRawSensorDataByTripIdUseCase {
        return FetchRawSensorDataByTripIdUseCase(unsafeBehaviourRepositoryImpl)
    }

}