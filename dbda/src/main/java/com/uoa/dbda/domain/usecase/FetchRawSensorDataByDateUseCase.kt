package com.uoa.dbda.domain.usecase

import android.util.Log
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class FetchRawSensorDataByDateUseCase @Inject constructor(
    private val sensorDataRepo: RawSensorDataRepository,
){
    suspend fun execute(startDate: LocalDate, endDate: LocalDate): Flow<List<RawSensorData>> {
        // Convert Long (epoch milliseconds) to LocalDate
//        val zoneId = ZoneId.systemDefault()
//        val sDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
//        val eDate = Instant.ofEpochMilli(endDate).atZone(zoneId).toLocalDate()

        return withContext(Dispatchers.IO) {
            sensorDataRepo.getSensorDataBetweenDates(startDate, endDate)
                .map { sensorDataList ->
                    Log.i("FetchRawSensorDataByDateUseCase", "Number of sensor data: ${sensorDataList.size}, startDate: $startDate, endDate: $endDate")
                    sensorDataList.map { it.toDomainModel() }
                }
        }
    }
}