package com.uoa.sensor.presentation.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.uoa.sensor.data.repository.LocationRepository
import com.uoa.sensor.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LocationManagerModule {
    // Provide the LocationManager implementation
    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    fun provideLocationManager(
        @ApplicationContext context: Context,
        locationRepository: LocationRepository,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationManager {
        // Provide an instance of LocationManager
        return LocationManager(context, locationRepository, fusedLocationProviderClient)
    }
}