package com.uoa.driverprofile.presentation.di

import com.uoa.core.database.daos.DriverProfileDAO
import com.uoa.core.database.daos.DrivingTipDao
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.database.repository.DrivingTipRepository
//import com.uoa.driverprofile.domain.usecase.DeleteAllDriverProfilesUseCase
import com.uoa.driverprofile.domain.usecase.DeleteDriverProfileByEmailUseCase
//import com.uoa.driverprofile.domain.usecase.GetAllDriverProfilesUseCase
import com.uoa.driverprofile.domain.usecase.GetDriverProfileByEmailUseCase
//import com.uoa.driverprofile.domain.usecase.GetDriverProfileByIdUseCase
//import com.uoa.driverprofile.domain.usecase.GetDriverProfileBySyncStatusUseCase
import com.uoa.driverprofile.domain.usecase.InsertDriverProfileUseCase
//import com.uoa.driverprofile.domain.usecase.UpdateDriverProfileUseCase
import com.uoa.driverprofile.repository.DriverProfileRepositoryImpl
import com.uoa.driverprofile.repository.DrivingTipRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProvideDriverProfileModule {
    @Provides
    fun provideDriverProfileRepository(driverProfileDAO: DriverProfileDAO): DriverProfileRepository {
        return DriverProfileRepositoryImpl(driverProfileDAO)
    }

    @Provides
    fun provideDrivingTipsRepository(driverProfileDAO: DrivingTipDao): DrivingTipRepository {
        return DrivingTipRepositoryImpl(driverProfileDAO)
    }

    @Provides
    fun provideInsertDriverProfileUseCase(driverProfileRepository: DriverProfileRepository): InsertDriverProfileUseCase {
        return InsertDriverProfileUseCase(driverProfileRepository)
    }

//    @Provides
//    fun provideUpdateDriverProfileUseCase(driverProfileRepository: DriverProfileRepository): UpdateDriverProfileUseCase {
//        return UpdateDriverProfileUseCase(driverProfileRepository)
//    }
//
//    @Provides
//    fun provideGetAllDriverProfilesUseCase(driverProfileRepository: DriverProfileRepository): GetAllDriverProfilesUseCase {
//        return GetAllDriverProfilesUseCase(driverProfileRepository)
//    }

//    @Provides
//    fun provideGetDriverProfileByIdUseCase(driverProfileRepository: DriverProfileRepository): GetDriverProfileByIdUseCase {
//        return GetDriverProfileByIdUseCase(driverProfileRepository)
//    }
//
//    @Provides
//    fun provideGetDriverProfileBySyncStatusUseCase(driverProfileRepository: DriverProfileRepository): GetDriverProfileBySyncStatusUseCase {
//        return GetDriverProfileBySyncStatusUseCase(driverProfileRepository)
//    }

    @Provides
    fun provideDeleteDriverProfileByEmailUseCase(driverProfileRepository: DriverProfileRepository): DeleteDriverProfileByEmailUseCase {
        return DeleteDriverProfileByEmailUseCase(driverProfileRepository)
    }

//    @Provides
//    fun provideDeleteAllDriverProfilesUseCase(driverProfileRepository: DriverProfileRepository): DeleteAllDriverProfilesUseCase {
//        return DeleteAllDriverProfilesUseCase(driverProfileRepository)
//    }

    @Provides
    fun provideGetDriverProfileByEmail(driverProfileRepository: DriverProfileRepository): GetDriverProfileByEmailUseCase {
        return GetDriverProfileByEmailUseCase(driverProfileRepository)
    }

}
