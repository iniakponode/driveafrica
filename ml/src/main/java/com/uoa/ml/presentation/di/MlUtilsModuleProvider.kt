package com.uoa.ml.presentation.di

import com.uoa.ml.Utils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MlUtilsModuleProvider {

    @Provides
    @Singleton
    fun provideUtils(): Utils {
        return Utils()
    }
}