package com.uoa.sensor.presentation.di

import android.content.Context
import com.uoa.core.Sdadb
import com.uoa.core.apiServices.services.roadApiService.RoadApiRepository
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.daos.TripSummaryDao
import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.ProcessAndStoreSensorData
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.TripFeatureStateRepository
import com.uoa.core.database.repository.TripSummaryRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.repository.RawSensorDataRepositoryImpl
import com.uoa.sensor.repository.RoadRepositoryImpl
import com.uoa.sensor.repository.DrivingStateStore
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.sensor.repository.TripDataRepositoryImpl
import com.uoa.sensor.repository.TripFeatureStateRepositoryImpl
import com.uoa.sensor.repository.TripSummaryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideRawSensorDataRepository(
        rawSensorDataDao: RawSensorDataDao,
       processAndStoreSensorData: ProcessAndStoreSensorData
    ): RawSensorDataRepository {
        return RawSensorDataRepositoryImpl(
            rawSensorDataDao = rawSensorDataDao,
            processAndStoreSensorData
        )
    }

//    //    provide LocationRepositoryImpl
    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao, rawSensorDataDao: RawSensorDataDao): LocationRepository =
        LocationRepositoryImpl(locationDao, rawSensorDataDao)

    @Provides
    @Singleton
    fun provideRoadRepository(roadDao: RoadDao, roadApiRepository: RoadApiRepository): RoadRepository =
        RoadRepositoryImpl(roadDao, roadApiRepository)

    @Provides
    @Singleton
    fun provideSensorDataColStateRepository(
        drivingStateStore: DrivingStateStore
    ): SensorDataColStateRepository =
        SensorDataColStateRepository(drivingStateStore)
//
//    //    provide TripRepository
    @Provides
    @Singleton
    fun provideTripRepository(tripDao: TripDao): TripDataRepository =
        TripDataRepositoryImpl(tripDao)

    @Provides
    @Singleton
    fun provideTripSummaryRepository(tripSummaryDao: TripSummaryDao): TripSummaryRepository =
        TripSummaryRepositoryImpl(tripSummaryDao)

    @Provides
    @Singleton
    fun provideTripFeatureStateRepository(
        tripFeatureStateDao: TripFeatureStateDao
    ): TripFeatureStateRepository = TripFeatureStateRepositoryImpl(tripFeatureStateDao)
}

//
// add UnsafeBehaviourReportRepository provider here
//@Provides
//fun provideUnsafeBehaviourRepository(unsafeBehaviourDao: UnsafeBehaviourDao): UnsafeBehaviourRepositoryImpl{
//    return UnsafeBehaviourRepositoryImpl(unsafeBehaviourDao)
//}

