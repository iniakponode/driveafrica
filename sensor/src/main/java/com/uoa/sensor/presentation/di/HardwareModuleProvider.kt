package com.uoa.sensor.presentation.di

import android.app.Application
import com.uoa.core.database.daos.RawSensorDataDao
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.repository.RawSensorDataRepositoryImpl
import com.uoa.sensor.hardware.AccelerometerSensor
import com.uoa.sensor.hardware.SensorDataBufferManager
import com.uoa.sensor.hardware.GravitySensor
import com.uoa.sensor.hardware.GyroscopeSensor
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.hardware.LinearAccelerationSensor
import com.uoa.sensor.hardware.MagnetometerSensor
import com.uoa.sensor.hardware.RotationVectorSensor
import com.uoa.sensor.hardware.SignificantMotion

import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.hardware.MotionDetector
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.repository.SensorDataColStateRepository
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

//    @Provides
//    @Singleton
//    fun provideRawSensorDataRepository(rawSensorDataDao: RawSensorDataDao): RawSensorDataRepositoryImpl {
//        // Provide an instance of RawSensorDataRepositoryImpl
//        return RawSensorDataRepositoryImpl(rawSensorDataDao)
//    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao, rawSensorDataDao: RawSensorDataDao): LocationRepositoryImpl {
        // Provide an instance of LocationRepositoryImpl
        return LocationRepositoryImpl(locationDao, rawSensorDataDao)
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
    fun provideLinearAcceleration(@ApplicationContext context: Context): LinearAccelerationSensor {
        // Provide an instance of LinearAcceleration
        return LinearAccelerationSensor(context)
    }

    @Provides
    @Singleton
    fun provideMotionDector(
        @SignificantMotionSensorM significantMotionSensor: SignificantMotion,
        @LinearAccelerationM linearAccelerationSensor: LinearAccelerationSensor,
        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor
    ): MotionDetector{
        return MotionDetector(
            significantMotionSensor,
            linearAccelerationSensor,
            accelerometerSensor
        )
    }

    @Provides
    @Singleton
    fun provideBufferManager(
        rawSensorDataRepository: RawSensorDataRepository,
//        unsafeBehaviourRepository: UnsafeBehaviourRepository,
//        newUnsafeDrivingBehaviourAnalyser: NewUnsafeDrivingBehaviourAnalyser,
//        @ApplicationContext context: Context
    ): SensorDataBufferManager {
        return SensorDataBufferManager(rawSensorDataRepository
//            ,unsafeBehaviourRepository, context, newUnsafeDrivingBehaviourAnalyser
        )
    }

    @Provides
    @Singleton
    fun provideHardwareModule(
        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor,
        @GyroscopeSensorM gyroscopeSensor: GyroscopeSensor,
        @RotationVectorSensorM rotationVectorSensor: RotationVectorSensor,
        @MagnetometerSensorM magnetometerSensor: MagnetometerSensor,
        @GravitySensorM gravitySensor: GravitySensor,
        @LinearAccelerationM linearAccelerationSensor: LinearAccelerationSensor,
        locationManager: LocationManager,
        locationBufferManager: LocationDataBufferManager,
        sensorDataBufferManager: SensorDataBufferManager,
        motionDetector: MotionDetector,
        sensorDataColStateRepository: SensorDataColStateRepository,
        aiModelInputRepository: AIModelInputRepository,
        locationRepository: LocationRepository,
        @ApplicationContext context: Context,
        updateTripUseCase: UpdateTripUseCase,
        rawSensorDataRepository: RawSensorDataRepository

        ): HardwareModule {
        return HardwareModule(
            accelerometerSensor,
            gyroscopeSensor,
            rotationVectorSensor,
            magnetometerSensor,
            gravitySensor,
            linearAccelerationSensor,
            locationBufferManager,
            locationManager,
            sensorDataBufferManager,
            motionDetector,
            aiModelInputRepository,
            locationRepository,
            context,
            sensorDataColStateRepository,
            rawSensorDataRepository



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