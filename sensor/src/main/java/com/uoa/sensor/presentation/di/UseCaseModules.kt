package com.uoa.sensor.presentation.di

import com.uoa.sensor.domain.usecases.trip.FetchTripUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.TripSummaryRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.sensor.repository.TripDataRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModules {
     @Provides
     fun provideFetchTripUseCase(tripRepository: TripDataRepositoryImpl): FetchTripUseCase {
         return FetchTripUseCase(tripRepository)
     }

     @Provides
     fun provideUpdateTripUseCase(
         tripRepository: TripDataRepository,
         tripSummaryRepository: TripSummaryRepository,
         locationRepository: LocationRepository,
         unsafeBehaviourRepository: UnsafeBehaviourRepository,
         aiModelInputRepository: AIModelInputRepository
     ): UpdateTripUseCase {
         return UpdateTripUseCase(
             tripRepository,
             tripSummaryRepository,
             locationRepository,
             unsafeBehaviourRepository,
             aiModelInputRepository
         )
     }

     @Provides
     fun provideInsertTripUseCase(tripRepository: TripDataRepositoryImpl): InsertTripUseCase {
         return InsertTripUseCase(tripRepository)
     }
}
