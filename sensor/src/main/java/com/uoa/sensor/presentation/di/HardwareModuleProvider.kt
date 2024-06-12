package com.uoa.sensor.presentation.di

import com.uoa.core.database.daos.RawSensorDataDao
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.uoa.core.database.daos.LocationDao
import com.uoa.sensor.data.repository.LocationRepository
import com.uoa.sensor.data.repository.RawSensorDataRepository
import com.uoa.sensor.hardware.AccelerometerSensor
import com.uoa.sensor.hardware.AccelerometerSensorM
import com.uoa.sensor.hardware.GravitySensor
import com.uoa.sensor.hardware.GravitySensorM
import com.uoa.sensor.hardware.GyroscopeSensor
import com.uoa.sensor.hardware.GyroscopeSensorM
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.hardware.LinearAcceleration
import com.uoa.sensor.hardware.LinearAccelerationM
import com.uoa.sensor.hardware.MagnetometerSensor
import com.uoa.sensor.hardware.MagnetometerSensorM
import com.uoa.sensor.hardware.RotationVectorSensor
import com.uoa.sensor.hardware.RotationVectorSensorM
import com.uoa.sensor.hardware.SignificantMotion
import com.uoa.sensor.hardware.SignificantMotionSensorM
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.hardware.ManageSensorDataSizeAndSave
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HardwareModuleProvider{

    @Provides
    @Singleton
    fun provideRawSensorDataRepository(rawSensorDataDao: RawSensorDataDao): RawSensorDataRepository {
        // Provide an instance of RawSensorDataRepository
        return RawSensorDataRepository(rawSensorDataDao)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao): LocationRepository {
        // Provide an instance of LocationRepository
        return LocationRepository(locationDao)
    }

    @Provides
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @AccelerometerSensorM
    fun provideAccelerometerSensor(@ApplicationContext context: Context): AccelerometerSensor {
        // Provide an instance of AccelerometerSensor
        return AccelerometerSensor(context)
    }

    @Provides
    @GyroscopeSensorM
    fun provideGyroscopeSensor(@ApplicationContext context: Context): GyroscopeSensor {
        // Provide an instance of GyroscopeSensor
        return GyroscopeSensor(context)
    }

    @Provides
    @RotationVectorSensorM
    fun provideRotationVectorSensor(@ApplicationContext context: Context): RotationVectorSensor {
        // Provide an instance of RotationVectorSensor
        return RotationVectorSensor(context)
    }

    @Provides
    @MagnetometerSensorM
    fun provideMagnetometerSensor(@ApplicationContext context: Context): MagnetometerSensor {
        // Provide an instance of MagnetometerSensor
        return MagnetometerSensor(context)
    }

    @Provides
    @SignificantMotionSensorM
    fun provideSignificantMotionSensor(@ApplicationContext context: Context): SignificantMotion {
        // Provide an instance of SignificantMotion
        return SignificantMotion(context)
    }
    @Provides
    @GravitySensorM
    fun provideGravitySensor(@ApplicationContext context: Context): GravitySensor {
        // Provide an instance of GravitySensor
        return GravitySensor(context)
    }

    @Provides
    @LinearAccelerationM
    fun provideLinearAcceleration(@ApplicationContext context: Context): LinearAcceleration {
        // Provide an instance of LinearAcceleration
        return LinearAcceleration(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideHardwareModule(
        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor,
        @GyroscopeSensorM gyroscopeSensor: GyroscopeSensor,
        @RotationVectorSensorM rotationVectorSensor: RotationVectorSensor,
        @MagnetometerSensorM magnetometerSensor: MagnetometerSensor,
        @SignificantMotionSensorM significantMotionSensor: SignificantMotion,
        @LinearAccelerationM linearAccelerationSensor: LinearAcceleration,
        @GravitySensorM gravitySensor: GravitySensor,
        locationManager: LocationManager,
        manageSensorDataSizeAndSave: ManageSensorDataSizeAndSave

    ): HardwareModule {
        return HardwareModule(
            accelerometerSensor,
            gyroscopeSensor,
            rotationVectorSensor,
            magnetometerSensor,
            significantMotionSensor,
            gravitySensor,
            linearAccelerationSensor,
            locationManager,
            manageSensorDataSizeAndSave
        )
    }
}