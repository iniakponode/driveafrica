package com.uoa.dbda.domain.usecase

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class FetchRawSensorDataByDateUseCase @Inject constructor(
    private val unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl,
){
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun execute(startDate: Long, endDate: Long): Flow<List<RawSensorData>> {
        // Convert Long (epoch milliseconds) to LocalDate
        val zoneId = ZoneId.systemDefault()
        val sDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
        val eDate = Instant.ofEpochMilli(endDate).atZone(zoneId).toLocalDate()

        return withContext(Dispatchers.IO) {
            unsafeBehaviourRepositoryImpl.getSensorDataBetweenDates(sDate, eDate)
                .map { sensorDataList ->
                    Log.i("FetchRawSensorDataByDateUseCase", "Number of sensor data: ${sensorDataList.size}, startDate: $sDate, endDate: $eDate")
                    sensorDataList.map { it.toDomainModel() }
                }
        }
    }
}