package com.uoa.sensor.presentation.di

import com.uoa.core.database.daos.RawSensorDataDao
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.hardware.AccelerometerSensor
import com.uoa.sensor.hardware.SensorDataBufferManager
import com.uoa.sensor.hardware.GravitySensor
import com.uoa.sensor.hardware.GyroscopeSensor
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.hardware.LinearAccelerationSensor
import com.uoa.sensor.hardware.MagnetometerSensor
import com.uoa.sensor.hardware.RotationVectorSensor
import com.uoa.sensor.hardware.SensorRecordingManager
import com.uoa.sensor.hardware.SignificantMotion
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.motion.DrivingStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HardwareModuleProvider{

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao, rawSensorDataDao: RawSensorDataDao): LocationRepositoryImpl {
        return LocationRepositoryImpl(locationDao, rawSensorDataDao)
    }

    @Provides
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @AccelerometerSensorM
    fun provideAccelerometerSensor(@ApplicationContext context: Context): AccelerometerSensor {
        return AccelerometerSensor(context)
    }

    @Provides
    @GyroscopeSensorM
    fun provideGyroscopeSensor(@ApplicationContext context: Context): GyroscopeSensor {
        return GyroscopeSensor(context)
    }

    @Provides
    @RotationVectorSensorM
    fun provideRotationVectorSensor(@ApplicationContext context: Context): RotationVectorSensor {
        return RotationVectorSensor(context)
    }

    @Provides
    @MagnetometerSensorM
    fun provideMagnetometerSensor(@ApplicationContext context: Context): MagnetometerSensor {
        return MagnetometerSensor(context)
    }

    @Provides
    @SignificantMotionSensorM
    fun provideSignificantMotionSensor(@ApplicationContext context: Context): SignificantMotion {
        return SignificantMotion(context)
    }
    @Provides
    @GravitySensorM
    fun provideGravitySensor(@ApplicationContext context: Context): GravitySensor {
        return GravitySensor(context)
    }

    @Provides
    @LinearAccelerationM
    fun provideLinearAcceleration(@ApplicationContext context: Context): LinearAccelerationSensor {
        return LinearAccelerationSensor(context)
    }

    @Provides
    @Singleton
    fun provideBufferManager(
        rawSensorDataRepository: RawSensorDataRepository,
    ): SensorDataBufferManager {
        return SensorDataBufferManager(rawSensorDataRepository)
    }

    @Provides
    @Singleton
    fun provideSensorRecordingManager(
        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor,
        @GyroscopeSensorM gyroscopeSensor: GyroscopeSensor,
        @RotationVectorSensorM rotationVectorSensor: RotationVectorSensor,
        @MagnetometerSensorM magnetometerSensor: MagnetometerSensor,
        @GravitySensorM gravitySensor: GravitySensor,
        @LinearAccelerationM linearAccelerationSensor: LinearAccelerationSensor,
        sensorDataBufferManager: SensorDataBufferManager,
        locationDataBufferManager: LocationDataBufferManager,
        @ApplicationContext context: Context
    ): SensorRecordingManager {
        return SensorRecordingManager(
            accelerometerSensor,
            gyroscopeSensor,
            rotationVectorSensor,
            magnetometerSensor,
            gravitySensor,
            linearAccelerationSensor,
            sensorDataBufferManager,
            locationDataBufferManager,
            context
        )
    }

    @Provides
    @Singleton
    fun provideHardwareModule(
        drivingStateManager: DrivingStateManager,
        locationManager: LocationManager,
        sensorRecordingManager: SensorRecordingManager
    ): HardwareModule {
        return HardwareModule(
            drivingStateManager,
            locationManager,
            sensorRecordingManager
        )
    }
}

@Qualifier
annotation class RotationVectorSensorM
@Qualifier
annotation class MagnetometerSensorM
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccelerometerSensorM

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GyroscopeSensorM

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LinearAccelerationM

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SignificantMotionSensorM

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GravitySensorM
