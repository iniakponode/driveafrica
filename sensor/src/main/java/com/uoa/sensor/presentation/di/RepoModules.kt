package com.uoa.sensor.presentation.di

import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.repository.RawSensorDataRepositoryImpl
import com.uoa.sensor.repository.TripDataRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//import com.uoa.core.database.daos.UnsafeBehaviourDao
////import com.uoa.sensor.repository.UnsafeBehaviourRepository
//import dagger.Provides
//import com.uoa.core.database.daos.LocationDao
//import com.uoa.core.database.daos.RawSensorDataDao
//import com.uoa.core.database.daos.TripDao
//import com.uoa.sensor.repository.LocationRepositoryImpl
//import com.uoa.sensor.repository.RawSensorDataRepositoryImpl
//import com.uoa.sensor.repository.TripDataRepositoryImpl
//import dagger.Module
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//
@Module
@InstallIn(SingletonComponent::class)
object RepoModules {
    // Add your RepoModule bindings here
//
    @Provides
    @Singleton
    fun provideRawSensorDataRepository(rawSensorDataDao: RawSensorDataDao): RawSensorDataRepository =
        RawSensorDataRepositoryImpl(rawSensorDataDao)

//    //    provide LocationRepositoryImpl
    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao, rawSensorDataDao: RawSensorDataDao): LocationRepository =
        LocationRepositoryImpl(locationDao, rawSensorDataDao)
//
//    //    provide TripRepository
    @Provides
    @Singleton
    fun provideTripRepository(tripDao: TripDao): TripDataRepository =
        TripDataRepositoryImpl(tripDao)
}

//
// add UnsafeBehaviourReportRepository provider here
//@Provides
//fun provideUnsafeBehaviourRepository(unsafeBehaviourDao: UnsafeBehaviourDao): UnsafeBehaviourRepositoryImpl{
//    return UnsafeBehaviourRepositoryImpl(unsafeBehaviourDao)
//}

