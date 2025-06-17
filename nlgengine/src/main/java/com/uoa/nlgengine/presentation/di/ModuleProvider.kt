package com.uoa.nlgengine.presentation.di

import com.uoa.core.database.daos.NLGReportDao
import com.uoa.core.database.daos.ReportStatisticsDao
import com.uoa.core.database.repository.NLGReportRepository
import com.uoa.core.database.repository.ReportStatisticsRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.utils.GetLastInsertedUnsafeBehaviourUseCase
import com.uoa.core.utils.UnsafeBehavioursBtwnDatesUseCase
import com.uoa.nlgengine.repository.NLGReportRepositoryImpl
import com.uoa.nlgengine.repository.ReportStatisticsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModuleProvider {
    @Provides
    @Singleton
    fun provideUnsafeBehavioursBtwnDatesUseCase(unsafeBehaviourRepository: UnsafeBehaviourRepository): UnsafeBehavioursBtwnDatesUseCase {
        return UnsafeBehavioursBtwnDatesUseCase(unsafeBehaviourRepository)
    }

    @Provides
    @Singleton
    fun providesGetLastInsertedUnsafeBehaviourUseCase(unsafeBehaviourRepository: UnsafeBehaviourRepository): GetLastInsertedUnsafeBehaviourUseCase {
        return GetLastInsertedUnsafeBehaviourUseCase(unsafeBehaviourRepository)
    }

    @Provides
    @Singleton
    fun providesReportStatistics(reportStatDao: ReportStatisticsDao): ReportStatisticsRepository{
        return ReportStatisticsRepositoryImpl(reportStatDao)
    }

    @Provides
    @Singleton
    fun provideNlgReport(nlgReportDao: NLGReportDao): NLGReportRepository{
        return NLGReportRepositoryImpl(nlgReportDao)
    }

//    @Provides
//    fun providesNLGEngineRepository(
//        @ChatGPTRetrofit chatGPTApiService: ChatGPTApiService,
//        @BaseRetrofit osmRoadApiService: OSMRoadApiService
//    ): NLGEngineRepository {
//        return NLGEngineRepositoryImpl(chatGPTApiService)
//    }


}