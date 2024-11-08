//package com.uoa.core.domain.sensor
//
//import com.uoa.core.database.repository.UnsafeBehaviourRepository
//import com.uoa.core.model.RawSensorData
//import com.uoa.core.utils.toDomainModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import java.time.Instant
//import java.time.ZoneId
//import android.util.Log
//import com.uoa.core.database.repository.RawSensorDataRepository
//import java.util.UUID
//import javax.inject.Inject
//
//class GetSensorDataByDate @Inject constructor(
//    private val sensorDataRepository: RawSensorDataRepository,
//) {
//
//    suspend fun execute(startDate: Long, endDate: Long): Flow<List<RawSensorData>> {
//        val zoneId = ZoneId.systemDefault()
//        val sDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
//        val eDate = Instant.ofEpochMilli(endDate).atZone(zoneId).toLocalDate()
//        val sensorDataList =
//            sensorDataRepository.getSensorDataBetweenDates(sDate, eDate)
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
//}