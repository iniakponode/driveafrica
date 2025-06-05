package com.uoa.sensor.presentation.di

import com.uoa.core.database.daos.RawSensorDataDao
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.entities.FFTFeatureDao
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.hardware.AccelerometerSensor
//import com.uoa.sensor.hardware.ActivityRecognitionManager
//import com.uoa.sensor.hardware.DataCollectionTrigger
import com.uoa.sensor.hardware.SensorDataBufferManager
import com.uoa.sensor.hardware.GravitySensor
import com.uoa.sensor.hardware.GyroscopeSensor
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.hardware.LinearAccelerationSensor
import com.uoa.sensor.hardware.MagnetometerSensor
import com.uoa.sensor.hardware.MotionDetection
import com.uoa.sensor.hardware.MotionDetectionFFT
import com.uoa.sensor.hardware.RotationVectorSensor
import com.uoa.sensor.hardware.SignificantMotion
//import com.uoa.sensor.hardware.VehicleMovementManager
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.presentation.viewModel.SensorViewModel
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
    fun provideMotionDection(
        @SignificantMotionSensorM significantMotionSensor: SignificantMotion,
        @LinearAccelerationM linearAccelerationSensor: LinearAccelerationSensor,
        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor,
        sensorDataColStateRepository: SensorDataColStateRepository
    ): MotionDetection{
        return MotionDetection(
            significantMotionSensor,
            linearAccelerationSensor,
            accelerometerSensor,
            sensorDataColStateRepository
        )
    }

    @Provides
    @Singleton
    fun provideMotionDetectionFFt(
        @SignificantMotionSensorM significantMotionSensor: SignificantMotion,
        @LinearAccelerationM linearAccelerationSensor: LinearAccelerationSensor,
        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor,
        fftFeatureDao: FFTFeatureDao,
        sensorDataColStateRepository: SensorDataColStateRepository
    ): MotionDetectionFFT{
        return MotionDetectionFFT(
            significantMotionSensor,
            linearAccelerationSensor,
            accelerometerSensor,
            fftFeatureDao,
            sensorDataColStateRepository
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
        motionDetector: MotionDetectionFFT,
        sensorDataColStateRepository: SensorDataColStateRepository,
        @ApplicationContext context: Context,

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
            sensorDataColStateRepository,
            context,

        )
    }


//    @Provides
//    @Singleton
//    fun provideHardwareModuleBackup(
//        @AccelerometerSensorM accelerometerSensor: AccelerometerSensor,
//        @GyroscopeSensorM gyroscopeSensor: GyroscopeSensor,
//        @RotationVectorSensorM rotationVectorSensor: RotationVectorSensor,
//        @MagnetometerSensorM magnetometerSensor: MagnetometerSensor,
//        @GravitySensorM gravitySensor: GravitySensor,
//        @LinearAccelerationM linearAccelerationSensor: LinearAccelerationSensor,
//        locationManager: LocationManagerBackup,
//        locationBufferManager: LocationDataBufferManager,
//        sensorDataBufferManager: SensorDataBufferManager,
////        vehicleMovementManager: VehicleMovementManager,
//        dataCollectionTrigger: DataCollectionTrigger,
//        sensorDataColStateRepository: SensorDataColStateRepository,
//        @ApplicationContext context: Context,
//
//    ): HardwareModuleBackUp {
//        return HardwareModuleBackUp(
//            accelerometerSensor,
//            gyroscopeSensor,
//            rotationVectorSensor,
//            magnetometerSensor,
//            gravitySensor,
//            linearAccelerationSensor,
//            locationBufferManager,
//            locationManager,
//            sensorDataBufferManager,
////            vehicleMovementManager,
//            dataCollectionTrigger,
//            sensorDataColStateRepository,
//            context
//        )
//    }

//    // Provide DataCollectionTrigger (formerly MotionDetector) that controls all sensor listeners.
//    @Provides
//    @Singleton
//    fun provideDataCollectionTrigger(
//        accelerometerSensor: AccelerometerSensor,
//        gyroscopeSensor: GyroscopeSensor,
//        rotationVectorSensor: RotationVectorSensor,
//        magnetometerSensor: MagnetometerSensor,
//        gravitySensor: GravitySensor,
//        linearAccelerationSensor: LinearAccelerationSensor
//    ): DataCollectionTrigger {
//        return DataCollectionTrigger(
//            accelerometerSensor,
//            gyroscopeSensor,
//            rotationVectorSensor,
//            magnetometerSensor,
//            gravitySensor,
//            linearAccelerationSensor
//        )
//    }

//    // Provide VehicleMovementManager which uses ActivityRecognition to trigger sensor collection.
//    @Provides
//    @Singleton
//    fun provideVehicleMovementManager(
//        activityRecognitionManager: ActivityRecognitionManager,
//        dataCollectionTrigger: DataCollectionTrigger,
//        @ApplicationContext appContext: Context,
//        sensorDataColStateRepository: SensorDataColStateRepository,
//    ): VehicleMovementManager {
//        return VehicleMovementManager(activityRecognitionManager, dataCollectionTrigger, appContext, sensorDataColStateRepository)
//    }
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