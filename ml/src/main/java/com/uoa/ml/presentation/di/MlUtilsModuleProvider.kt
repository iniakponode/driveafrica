package com.uoa.ml.presentation.di

import com.uoa.core.mlclassifier.MinMaxValuesLoader
//import com.uoa.ml.Utils
import com.uoa.ml.UtilsNew
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MlUtilsModuleProvider {

//    @Provides
//    @Singleton
//    fun provideUtils(): Utils {
//        return Utils()
//    }

    @Provides
    @Singleton
    fun provideUtilsNew(minMaxValuesLoader: MinMaxValuesLoader): UtilsNew {
        return UtilsNew(minMaxValuesLoader)
    }
}