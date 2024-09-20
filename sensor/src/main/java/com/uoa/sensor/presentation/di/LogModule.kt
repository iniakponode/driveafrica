package com.uoa.sensor.presentation.di

import android.util.Log
import com.uoa.sensor.LogFunction
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object LogModule {

    @Provides
    fun provideLogFunction(): LogFunction = { tag, msg -> Log.e(tag, msg) }

//    @Provides
//    fun provideLogFunction2(): (String, String) -> Unit = { tag, msg -> Log.d(tag, msg) }
//    @Provides
//    fun provideLogFunction3(): (String, String) -> Unit = { tag, msg -> Log.i(tag, msg) }
    }