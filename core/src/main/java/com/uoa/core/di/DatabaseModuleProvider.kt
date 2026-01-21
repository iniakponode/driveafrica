package com.uoa.core.di

import android.app.Application
import androidx.room.Room
import com.uoa.core.Sdadb
import com.uoa.core.database.MIGRATION_40_41
import com.uoa.core.database.MIGRATION_41_42
import com.uoa.core.database.MIGRATION_42_43
import com.uoa.core.database.MIGRATION_43_44
import com.uoa.core.database.MIGRATION_44_45
import com.uoa.core.database.MIGRATION_45_46
import com.uoa.core.database.MIGRATION_46_47
import com.uoa.core.database.MIGRATION_47_48
import com.uoa.core.database.MIGRATION_48_49
import com.uoa.core.database.MIGRATION_49_50
import com.uoa.core.database.MIGRATION_50_51
import com.uoa.core.database.MIGRATION_51_52
import com.uoa.core.database.MIGRATION_52_53
import com.uoa.core.database.daos.TripFeatureStateDao
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
            .addMigrations(
                MIGRATION_40_41,
                MIGRATION_41_42,
                MIGRATION_42_43,
                MIGRATION_43_44,
                MIGRATION_44_45,
                MIGRATION_45_46,
                MIGRATION_46_47,
                MIGRATION_47_48,
                MIGRATION_48_49,
                MIGRATION_49_50,
                MIGRATION_50_51,
                MIGRATION_51_52,
                MIGRATION_52_53
            )
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

    @Provides
    @Singleton
    fun provideTripSummaryDao(db: Sdadb) = db.tripSummaryDao()

    @Provides
    @Singleton
    fun provideTripFeatureStateDao(db: Sdadb): TripFeatureStateDao = db.tripFeatureStateDao()

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
