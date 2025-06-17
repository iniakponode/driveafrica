package com.uoa.core.di

import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.repository.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BehaviourAnalysisModule {
//    @Provides
//    fun provideBehaviourAnalysis(): UnsafeBehaviorAnalyser {
//        return UnsafeBehaviorAnalyser()
//    }

    @Provides
    @Singleton
    fun provideNewBehaviourAnalysis(locationRepository: LocationRepository): NewUnsafeDrivingBehaviourAnalyser {
        return NewUnsafeDrivingBehaviourAnalyser(locationRepository)
    }
}