package com.uoa.dbda.domain.usecase


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
//import com.uoa.dbda.domain.usecase.analyser.UnsafeBehaviorAnalyser
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

//class AnalyzeUnsafeBehaviorUseCase @Inject constructor(
//    private val unsafeBehaviorAnalyser: UnsafeBehaviorAnalyser,
//    private val unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl,
//) {
//
//    suspend fun execute(startDate: Long, endDate: Long): Flow<List<RawSensorData>> {
//        val zoneId = ZoneId.systemDefault()
//        val sDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
//        val eDate = Instant.ofEpochMilli(endDate).atZone(zoneId).toLocalDate()
//        val sensorDataList =
//            unsafeBehaviourRepositoryImpl.getSensorDataBetweenDates(sDate, eDate)
//        Log.i(
//            "AnalyzeUnsafeBehaviorUseCase", "Number of sensor data: ${
//                sensorDataList.collect {
//                    it.size
//                }
//            }, startDate: ${sDate}, endDate: $eDate"
//        )
//
////        sensorDataList.collect{ sensorDataEntities ->
////            val unsafeBehaviours = unsafeBehaviorAnalyser.analyse(sensorDataEntities)
////            Log.i("AnalyzeUnsafeBehaviorUseCase", "Number of unsafe behaviours: ${unsafeBehaviours.size}")
////            unsafeBehaviourRepositoryImpl.insertUnsafeBehaviourBatch(unsafeBehaviours)
////            }
//        return sensorDataList.map { sensorDList ->
//            sensorDList.map { it.toDomainModel() }
//        }
//    }


//    suspend fun analyseByTrip(tripId: UUID) {
//        val sensorDataList = unsafeBehaviourRepositoryImpl.getSensorDataByTripId(tripId)
//
//        sensorDataList.collect{ sensorDataEntities ->
//            val unsafeBehaviours = unsafeBehaviorAnalyser.analyse(sensorDataEntities)
//            unsafeBehaviourRepositoryImpl.insertUnsafeBehaviourBatch(unsafeBehaviours)
//            }
//        }
//
//
//}
