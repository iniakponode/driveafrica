package com.uoa.driverprofile.presentation.di
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByIdUseCase
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByProfileIdUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object ProvideDrivingTipsModule {

    @Provides
    fun provideGetDrivingTipByProfileIdUseCase(drivingTipRepository: DrivingTipRepository): GetDrivingTipByProfileIdUseCase {
        return GetDrivingTipByProfileIdUseCase(drivingTipRepository)
    }

    @Provides
    fun provideGetDrivingTipByIdUseCase(drivingTipRepository: DrivingTipRepository): GetDrivingTipByIdUseCase {
        return GetDrivingTipByIdUseCase(drivingTipRepository)
    }
}