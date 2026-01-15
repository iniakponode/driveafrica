package com.uoa.core.di

import android.content.Context
import com.uoa.core.Sdadb
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.ProcessAndStoreSensorData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorProcessingModule {

    @Provides
    @Singleton
    fun provideProcessAndStoreSensorData(
        rawSensorDataDao: RawSensorDataDao,
        appDatabase: Sdadb,
        @ApplicationContext context: Context,
        unsafeDrivingAnalyser: NewUnsafeDrivingBehaviourAnalyser,
        aiModelInputRepository: AIModelInputRepository,
        locationDao: LocationDao,
        unsafeBehaviourDao: UnsafeBehaviourDao,
        tripFeatureStateDao: TripFeatureStateDao
    ): ProcessAndStoreSensorData {
        return ProcessAndStoreSensorData(
            rawSensorDataDao = rawSensorDataDao,
            appDatabase = appDatabase,
            context = context,
            unsafeDrivingAnalyser = unsafeDrivingAnalyser,
            aiModelInputRepository = aiModelInputRepository,
            locationDao = locationDao,
            unsafeBehaviourDao = unsafeBehaviourDao,
            tripFeatureStateDao = tripFeatureStateDao
        )
    }
}
