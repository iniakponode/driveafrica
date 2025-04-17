package com.uoa.core

import com.uoa.core.database.daos.RawSensorDataDao
import androidx.room.*
//import androidx.room.RoomDatabase
//import androidx.room.TypeConverters
import com.uoa.core.database.daos.AIModelInputDao
import com.uoa.core.database.daos.CauseDao
//import com.uoa.core.database.daos.DbdaResultDao
import com.uoa.core.database.daos.DriverProfileDAO
import com.uoa.core.database.daos.DrivingTipDao
import com.uoa.core.database.daos.EmbeddingDao
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.NLGReportDao
import com.uoa.core.database.daos.AlcoholQuestionnaireResponseDao
import com.uoa.core.database.daos.ReportStatisticsDao
import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.daos.SensorDataDao
import com.uoa.core.database.entities.SensorEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.entities.DrivingTipEntity
import com.uoa.core.database.entities.EmbeddingEntity
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.QuestionnaireEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.ReportStatisticsEntity
import com.uoa.core.database.entities.RoadEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity

//import com.uoa.core.database.converters.Converters

@Database(entities = [RawSensorDataEntity::class,
                        AIModelInputsEntity::class,
                        SensorEntity::class,
                        TripEntity::class,
//                        DbdaResultEntity::class,
                        NLGReportEntity::class,
                        LocationEntity::class,
                        UnsafeBehaviourEntity::class,
                        DriverProfileEntity::class,
                        DrivingTipEntity::class,
                        CauseEntity::class,
                        RoadEntity::class,
                        EmbeddingEntity::class,
                        ReportStatisticsEntity::class,
                        QuestionnaireEntity::class],
                        version = 39)
@TypeConverters(Converters::class)
abstract class Sdadb : RoomDatabase() {
    abstract fun sensorDataDao(): SensorDataDao
//    abstract fun dbdaResultDao(): DbdaResultDao
    abstract fun nlgReportDao(): NLGReportDao
    abstract fun tripDao(): TripDao
    abstract fun rawSensorDataDao(): RawSensorDataDao
    abstract fun locationDataDao(): LocationDao
    abstract fun unsafeBehaviourDao(): UnsafeBehaviourDao
    abstract fun driverProfileDao(): DriverProfileDAO
    abstract fun drivingTipDao(): DrivingTipDao
    abstract fun causeDao(): CauseDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun roadDao(): RoadDao
    abstract fun aiModelInputsDao(): AIModelInputDao
    abstract fun reportStatisticsDao(): ReportStatisticsDao
    abstract fun alcoholQuestionnaireDao(): AlcoholQuestionnaireResponseDao
//...
}
