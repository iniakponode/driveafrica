package com.uoa.sensor.presentation.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LocationManagerModule {
    // Provide the LocationManager implementation
    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    fun provideLocationManager(
//        @ApplicationContext context: Context,
        locationRepositoryImpl: LocationRepositoryImpl,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationManager {
        // Provide an instance of LocationManager
        return LocationManager(locationRepositoryImpl, fusedLocationProviderClient)
    }
}