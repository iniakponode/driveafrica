package com.uoa.core.di

import android.content.Context
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MinMaxModule {

    @Provides
    @Singleton
    fun provideMinMaxValuesLoader(@ApplicationContext context: Context): MinMaxValuesLoader {
        return MinMaxValuesLoader(context)
    }
}
