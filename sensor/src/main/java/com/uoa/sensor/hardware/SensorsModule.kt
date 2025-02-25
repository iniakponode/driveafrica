package com.uoa.sensor.hardware

import android.app.Application
import android.content.Context
import com.uoa.sensor.hardware.base.AndroidSensor
import com.uoa.sensor.hardware.base.SignificantMotionSensor
import com.uoa.sensor.hardware.base.TrackingSensor
import com.uoa.sensor.hardware.base.TriggerTrackingSensor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorsModule {

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class TrackingSensorModule {
        @Binds
        @Singleton
        abstract fun bindTrackingSensor(androidSensor: AndroidSensor): TrackingSensor
    }

//    @Provides
//    @SignificantMotionSensorM // Use the qualifier if needed
//    fun provideSignificantMotionSensor(
//        @ApplicationContext context: Context
//    ): TriggerTrackingSensor {
//        return SignificantMotionSensor(context)
//    }

    @Provides
    @Singleton
    @AccelerometerSensorM
    fun provideAccelerometerSensor(app: Application): TrackingSensor {
        return AccelerometerSensor(app)
    }

    @Provides
    @Singleton
    @AccelerometerSensorM
    fun provideLinearAcceleration(app: Application): TrackingSensor {
        return LinearAccelerationSensor(app)
    }

    @Provides
    @Singleton
    @GyroscopeSensorM
    fun provideGyroscopeSensor(app: Application): TrackingSensor {
        return GyroscopeSensor(app)
    }


//    @Provides
//    @Singleton
//    @LinearAccelerationM
//    fun provideLinearAcceleration(app: Application): TrackingSensor {
//        return LinearAcceleration(app)
//    }


    @Provides
    @Singleton
    @RotationVectorSensorM
    fun provideRotationVectorSensor(app: Application): TrackingSensor {
        return RotationVectorSensor(app)
    }

    @Provides
    @Singleton
    @MagnetometerSensorM
    fun provideMagnetometerSensor(app: Application): TrackingSensor {
        return MagnetometerSensor(app)
    }

    @Provides
    @Singleton
    @SignificantMotionSensorM
    fun provideSignificantMotionSensor(app: Application): TriggerTrackingSensor {
        return SignificantMotion(app)
    }

    @Provides
    @Singleton
    @GravitySensorM
    fun provideGravitySensor(app: Application): TrackingSensor {
        return GravitySensor(app)
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
