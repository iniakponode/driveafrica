package com.uoa.sensor.presentation.di

import com.uoa.sensor.repository.TripDataRepositoryImpl
import com.uoa.sensor.domain.usecases.trip.FetchTripUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
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
     fun provideUpdateTripUseCase(tripRepository: TripDataRepositoryImpl): UpdateTripUseCase {
         return UpdateTripUseCase(tripRepository)
     }

     @Provides
     fun provideInsertTripUseCase(tripRepository: TripDataRepositoryImpl): InsertTripUseCase {
         return InsertTripUseCase(tripRepository)
     }
}