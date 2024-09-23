package com.uoa.core.di

import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BehaviourAnalysisModule {
    @Provides
    fun provideBehaviourAnalysis(): UnsafeBehaviorAnalyser {
        return UnsafeBehaviorAnalyser()
    }
}