package com.uoa.core.di

import android.app.Application
import androidx.room.Room
import com.uoa.core.Sdaddb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModuleProvider {

    @Provides
    @Singleton
    fun provideDatabaseModule(app: Application): Sdaddb {
        return Room.databaseBuilder(app, Sdaddb::class.java, "sda-db")
//            .setQueryCallback(RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
//                Log.d("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
//            }, Executors.newSingleThreadExecutor())
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSensorDataDao(db: Sdaddb) = db.sensorDataDao()

    @Provides
    @Singleton
    fun provideAIModelInputDao(db: Sdaddb) = db.aiModelInputsDao()


    @Provides
    @Singleton
    fun provideRoadDao(db: Sdaddb) = db.roadDao()

    @Provides
    @Singleton
    fun provideDbdaResultDao(db: Sdaddb) = db.dbdaResultDao()

    @Provides
    @Singleton
    fun provideEmbeddingDao(db: Sdaddb) = db.embeddingDao()

    @Provides
    @Singleton
    fun provideNlgReportDao(db: Sdaddb) = db.nlgReportDao()

    @Provides
    @Singleton
    fun provideTripDao(db: Sdaddb) = db.tripDao()

    @Provides
    @Singleton
    fun provideRawsensorDataDao(db: Sdaddb) = db.rawSensorDataDao()

    @Provides
    @Singleton
    fun provideLocationDataDao(db: Sdaddb) = db.locationDataDao()

    @Provides
    @Singleton
    fun provideDriverProfileDao(db: Sdaddb) = db.driverProfileDao()

    @Provides
    @Singleton
    fun provideDrivingTipDao(db: Sdaddb) = db.drivingTipDao()

    @Provides
    @Singleton
    fun provideUnsafeBehaviourDao(db: Sdaddb) = db.unsafeBehaviourDao()

    @Provides
    @Singleton
    fun provideCauseDao(db: Sdaddb) = db.causeDao()

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
