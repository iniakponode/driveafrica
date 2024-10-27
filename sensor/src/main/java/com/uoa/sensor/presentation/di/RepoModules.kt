package com.uoa.sensor.presentation.di

import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.repository.RawSensorDataRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
    fun provideRawSensorDataRepository(rawSensorDataDao: RawSensorDataDao): RawSensorDataRepository =
        RawSensorDataRepositoryImpl(rawSensorDataDao)

//    //    provide LocationRepositoryImpl
    @Provides
    fun provideLocationRepository(locationDao: LocationDao, rawSensorDataDao: RawSensorDataDao): LocationRepository =
        LocationRepositoryImpl(locationDao, rawSensorDataDao)
//
//    //    provide TripRepository
//    @Provides
//    fun provideTripRepository(tripDao: TripDao): TripDataRepositoryImpl =
//        TripDataRepositoryImpl(tripDao)
//}

//
// add UnsafeBehaviourReportRepository provider here
//@Provides
//fun provideUnsafeBehaviourRepository(unsafeBehaviourDao: UnsafeBehaviourDao): UnsafeBehaviourRepository {
//    return UnsafeBehaviourRepository(unsafeBehaviourDao)
}

