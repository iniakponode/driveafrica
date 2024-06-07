//package com.uoa.sensor.presentation.di
//
//import com.uoa.core.database.daos.LocationDao
//import com.uoa.core.database.daos.RawSensorDataDao
//import com.uoa.core.database.daos.SensorDataDao
//import com.uoa.core.database.daos.TripDao
//import com.uoa.sensor.data.repository.LocationRepository
//import com.uoa.sensor.data.repository.RawSensorDataRepository
//import com.uoa.sensor.data.repository.SensorDataRepository
//import com.uoa.sensor.data.repository.TripDataRepository
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//
//@Module
//@InstallIn(SingletonComponent::class)
//object RepoModules {
//    // Add your RepoModule bindings here
//
//    @Provides
//    fun provideRawSensorDataRepository(rawSensorDataDao: RawSensorDataDao): RawSensorDataRepository = RawSensorDataRepository(rawSensorDataDao)
//
////    provide LocationRepository
//    @Provides
//    fun provideLocationRepository(locationDao: LocationDao): LocationRepository = LocationRepository(locationDao)
//
////    provide TripRepository
//    @Provides
//    fun provideTripRepository(tripDao: TripDao): TripDataRepository = TripDataRepository(tripDao)
//
//
//}