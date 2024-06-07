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
            .fallbackToDestructiveMigration()
            .build()
    }
    @Provides
    @Singleton
    fun provideSensorDataDao(db: Sdaddb) = db.sensorDataDao()

    @Provides
    @Singleton
    fun provideDbdaResultDao(db: Sdaddb) = db.dbdaResultDao()

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
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SensorDataDaoC

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DbdaResultDaoC

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NlgReportDaoC

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TripDaoC

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocationDataDaoC