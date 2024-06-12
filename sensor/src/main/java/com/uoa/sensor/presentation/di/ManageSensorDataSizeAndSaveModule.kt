package com.uoa.sensor.presentation.di

import com.uoa.sensor.data.repository.RawSensorDataRepository
import com.uoa.sensor.hardware.ManageSensorDataSizeAndSave
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManageSensorDataSizeAndSaveModule {
     @Provides
     @Singleton
     fun provideManageSensorDataSizeAndSave(rawSensorDataRepository: RawSensorDataRepository): ManageSensorDataSizeAndSave {
         return ManageSensorDataSizeAndSave(rawSensorDataRepository)
     }
}