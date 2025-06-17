package com.uoa.core.di

import android.content.Context
import com.uoa.core.notifications.VehicleNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VehicleNotificationModule {
    @Provides
    @Singleton
    fun provideVehicleNotificationManager(@ApplicationContext context: Context): VehicleNotificationManager{
        return VehicleNotificationManager(context)
    }
}