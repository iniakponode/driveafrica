//package com.uoa.core.domain.sensor
//
//import com.uoa.core.database.repository.RawSensorDataRepository
//import com.uoa.core.model.RawSensorData
//import com.uoa.core.utils.toDomainModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import java.util.UUID
//
//class GetSensorDataByTripID(
//    val sensorDataRepository: RawSensorDataRepository
//) {
//    suspend fun invoke(tripId: UUID): Flow<List<RawSensorData>> {
//        val sensorDataList = sensorDataRepository.getSensorDataByTripId(tripId)
//
//        return sensorDataList.map { sensorDList ->
//            sensorDList.map { it.toDomainModel() }
//        }
//        }
//}