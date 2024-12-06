package com.uoa.sensor.presentation.di

import com.uoa.sensor.hardware.HardwareModule
//import com.uoa.sensor.worker.SensorWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

//@Module
//@InstallIn(SingletonComponent::class)
//object SensorWorkerModule {
//    // Add your WorkerModule bindings here
//    @Provides
//    fun provideSensorWorker(@ApplicationContext context: ApplicationContext, hardwareModule: HardwareModule) = SensorWorker(context,hardwareModule)
//}