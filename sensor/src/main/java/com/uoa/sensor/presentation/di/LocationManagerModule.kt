package com.uoa.sensor.presentation.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
import com.uoa.sensor.hardware.MotionDetector
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationManagerModule {
    // Provide the LocationManager implementation
    @Provides
    fun provideLocationManager(
//        @ApplicationContext context: Context,
        locationDataBufferManager: LocationDataBufferManager,
        fusedLocationProviderClient: FusedLocationProviderClient,
        motionDector: MotionDetector,
        osmSpeedLimitApiService: OSMSpeedLimitApiService
    ): LocationManager {
        // Provide an instance of LocationManager
        return LocationManager(locationDataBufferManager, fusedLocationProviderClient, motionDector,osmSpeedLimitApiService)
    }

    @Provides
    @Singleton
    fun provideLocationDataBufferManager(
        locationRepositoryImpl: LocationRepository
    ): LocationDataBufferManager {
        return LocationDataBufferManager(locationRepositoryImpl)
    }
}