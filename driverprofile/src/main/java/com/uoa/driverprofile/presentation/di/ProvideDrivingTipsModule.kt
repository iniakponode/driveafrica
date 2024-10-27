package com.uoa.driverprofile.presentation.di
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByIdUseCase
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByProfileIdUseCase
import com.uoa.driverprofile.domain.usecase.GetUnsafeBehavioursForTipsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ProvideDrivingTipsModule {

    @Provides
    @Singleton
    fun provideGetDrivingTipByProfileIdUseCase(drivingTipRepository: DrivingTipRepository): GetDrivingTipByProfileIdUseCase {
        return GetDrivingTipByProfileIdUseCase(drivingTipRepository)
    }

    @Provides
    @Singleton
    fun provideGetDrivingTipByIdUseCase(drivingTipRepository: DrivingTipRepository): GetDrivingTipByIdUseCase {
        return GetDrivingTipByIdUseCase(drivingTipRepository)
    }

    @Provides
    @Singleton
    fun provideGetUnsafeBehavioursForTipsUseCase(unsafeBehaviourRepository: UnsafeBehaviourRepository): GetUnsafeBehavioursForTipsUseCase {
        return GetUnsafeBehavioursForTipsUseCase(unsafeBehaviourRepository)
    }
}