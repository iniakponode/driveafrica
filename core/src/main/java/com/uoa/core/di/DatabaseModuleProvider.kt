package com.uoa.core.di

import android.app.Application
import androidx.room.Room
import com.uoa.core.Sdadb
import com.uoa.core.database.entities.FFTFeatureDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModuleProvider {

    @Provides
    @Singleton
    fun provideDatabaseModule(app: Application): Sdadb {
        return Room.databaseBuilder(app, Sdadb::class.java, "sda-db")
//            .setQueryCallback(RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
//                Log.d("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
//            }, Executors.newSingleThreadExecutor())
            .fallbackToDestructiveMigration()
            .build()
    }
    @Provides
    fun provideFftFeatureDao(db: Sdadb): FFTFeatureDao =
        db.fftfeaturesDao()

    @Provides
    @Singleton
    fun provideSensorDataDao(db: Sdadb) = db.sensorDataDao()

    @Provides
    @Singleton
    fun provideAIModelInputDao(db: Sdadb) = db.aiModelInputsDao()

    @Provides
    @Singleton
    fun provideQuestionnaireDao(db: Sdadb) = db.alcoholQuestionnaireDao()

    @Provides
    @Singleton
    fun provideRoadDao(db: Sdadb) = db.roadDao()

//    @Provides
//    @Singleton
//    fun provideDbdaResultDao(db: Sdadb) = db.dbdaResultDao()

    @Provides
    @Singleton
    fun provideEmbeddingDao(db: Sdadb) = db.embeddingDao()

    @Provides
    @Singleton
    fun provideNlgReportDao(db: Sdadb) = db.nlgReportDao()

    @Provides
    @Singleton
    fun provideTripDao(db: Sdadb) = db.tripDao()

    @Provides
    @Singleton
    fun provideRawsensorDataDao(db: Sdadb) = db.rawSensorDataDao()

    @Provides
    @Singleton
    fun provideLocationDataDao(db: Sdadb) = db.locationDataDao()

    @Provides
    @Singleton
    fun provideDriverProfileDao(db: Sdadb) = db.driverProfileDao()

    @Provides
    @Singleton
    fun provideDrivingTipDao(db: Sdadb) = db.drivingTipDao()

    @Provides
    @Singleton
    fun provideUnsafeBehaviourDao(db: Sdadb) = db.unsafeBehaviourDao()

    @Provides
    @Singleton
    fun provideCauseDao(db: Sdadb) = db.causeDao()

    @Provides
    @Singleton
    fun provideReportStatisticsDao(db: Sdadb) = db.reportStatisticsDao()

}

//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class SensorDataDaoC
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class DbdaResultDaoC
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class NlgReportDaoC
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class TripDaoC
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class LocationDataDaoC
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class UnsafeBehaviourDaoC
