package com.uoa.sensor.presentation.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.network.apiservices.OSMRoadApiService
import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationManagerModule {
    // Provide the LocationManager implementation
    @Provides
    fun provideLocationManager(
        @ApplicationContext context: Context,
        locationDataBufferManager: LocationDataBufferManager,
        fusedLocationProviderClient: FusedLocationProviderClient,
        osmSpeedLimitApiService: OSMSpeedLimitApiService,
        osmRoadApiService: OSMRoadApiService,
        roadRepository: RoadRepository,
        sensorDataColStateRepository: SensorDataColStateRepository
    ): LocationManager {
        // Provide an instance of LocationManager
        return LocationManager(
            locationDataBufferManager,
            fusedLocationProviderClient,
            osmSpeedLimitApiService,
            context,
            osmRoadApiService,
            roadRepository,
            sensorDataColStateRepository
        )
    }

//    @Provides
//    @Singleton
//    fun provideLocationManager(
//        bufferManager: LocationDataBufferManager,
//        fusedLocationProviderClient: FusedLocationProviderClient,
//        osmSpeedLimitApiService: OSMSpeedLimitApiService,
//        sensorDataColRepository: SensorDataColStateRepository,
////        vehicleMovementManager: VehicleMovementManager
//    ): LocationManagerBackup {
//        return LocationManagerBackup(
//            bufferManager = bufferManager,
//            fusedLocationProviderClient = fusedLocationProviderClient,
//            osmSpeedLimitApiService = osmSpeedLimitApiService,
//            sensorDataColRepository
////            vehicleMovementManager = vehicleMovementManager
//        )
//    }

    @Provides
    @Singleton
    fun provideLocationDataBufferManager(
        locationRepositoryImpl: LocationRepository
    ): LocationDataBufferManager {
        return LocationDataBufferManager(locationRepositoryImpl)
    }
}
