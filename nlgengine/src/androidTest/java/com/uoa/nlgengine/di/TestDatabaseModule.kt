package com.uoa.nlgengine.di

import android.content.Context
import androidx.room.Room
import com.uoa.core.Sdadb
import com.uoa.core.database.daos.AIModelInputDao
import com.uoa.core.database.daos.AlcoholQuestionnaireResponseDao
import com.uoa.core.database.daos.CauseDao
import com.uoa.core.database.daos.DriverProfileDAO
import com.uoa.core.database.daos.DrivingTipDao
import com.uoa.core.database.daos.EmbeddingDao
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.NLGReportDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.ReportStatisticsDao
import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.daos.SensorDataDao
import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.daos.TripSummaryDao
import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.entities.FFTFeatureDao
import com.uoa.core.di.DatabaseModuleProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModuleProvider::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseModule(@ApplicationContext context: Context): Sdadb {
        return Room.inMemoryDatabaseBuilder(context, Sdadb::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideFftFeatureDao(db: Sdadb): FFTFeatureDao =
        db.fftfeaturesDao()

    @Provides
    @Singleton
    fun provideSensorDataDao(db: Sdadb): SensorDataDao = db.sensorDataDao()

    @Provides
    @Singleton
    fun provideAIModelInputDao(db: Sdadb): AIModelInputDao = db.aiModelInputsDao()

    @Provides
    @Singleton
    fun provideQuestionnaireDao(db: Sdadb): AlcoholQuestionnaireResponseDao =
        db.alcoholQuestionnaireDao()

    @Provides
    @Singleton
    fun provideRoadDao(db: Sdadb): RoadDao = db.roadDao()

    @Provides
    @Singleton
    fun provideEmbeddingDao(db: Sdadb): EmbeddingDao = db.embeddingDao()

    @Provides
    @Singleton
    fun provideNlgReportDao(db: Sdadb): NLGReportDao = db.nlgReportDao()

    @Provides
    @Singleton
    fun provideTripDao(db: Sdadb): TripDao = db.tripDao()

    @Provides
    @Singleton
    fun provideTripSummaryDao(db: Sdadb): TripSummaryDao = db.tripSummaryDao()

    @Provides
    @Singleton
    fun provideTripFeatureStateDao(db: Sdadb): TripFeatureStateDao = db.tripFeatureStateDao()

    @Provides
    @Singleton
    fun provideRawsensorDataDao(db: Sdadb): RawSensorDataDao = db.rawSensorDataDao()

    @Provides
    @Singleton
    fun provideLocationDataDao(db: Sdadb): LocationDao = db.locationDataDao()

    @Provides
    @Singleton
    fun provideDriverProfileDao(db: Sdadb): DriverProfileDAO = db.driverProfileDao()

    @Provides
    @Singleton
    fun provideDrivingTipDao(db: Sdadb): DrivingTipDao = db.drivingTipDao()

    @Provides
    @Singleton
    fun provideUnsafeBehaviourDao(db: Sdadb): UnsafeBehaviourDao = db.unsafeBehaviourDao()

    @Provides
    @Singleton
    fun provideCauseDao(db: Sdadb): CauseDao = db.causeDao()

    @Provides
    @Singleton
    fun provideReportStatisticsDao(db: Sdadb): ReportStatisticsDao = db.reportStatisticsDao()
}
