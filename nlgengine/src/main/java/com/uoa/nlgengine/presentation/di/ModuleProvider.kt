package com.uoa.nlgengine.presentation.di

import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.nlgengine.domain.usecases.local.GetLastInsertedUnsafeBehaviourUseCase
import com.uoa.nlgengine.domain.usecases.local.UnsafeBehavioursBtwnDatesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ModuleProvider {
    @Provides
    fun provideUnsafeBehavioursBtwnDatesUseCase(unsafeBehaviourRepository: UnsafeBehaviourRepository): UnsafeBehavioursBtwnDatesUseCase {
        return UnsafeBehavioursBtwnDatesUseCase(unsafeBehaviourRepository)
    }

    @Provides
    fun providesGetLastInsertedUnsafeBehaviourUseCase(unsafeBehaviourRepository: UnsafeBehaviourRepository): GetLastInsertedUnsafeBehaviourUseCase {
        return GetLastInsertedUnsafeBehaviourUseCase(unsafeBehaviourRepository)
    }

//    @Provides
//    fun providesNLGEngineRepository(
//        @ChatGPTRetrofit chatGPTApiService: ChatGPTApiService,
//        @BaseRetrofit osmRoadApiService: OSMApiService
//    ): NLGEngineRepository {
//        return NLGEngineRepositoryImpl(chatGPTApiService)
//    }


}