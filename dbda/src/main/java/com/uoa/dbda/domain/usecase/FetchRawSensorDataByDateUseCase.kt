package com.uoa.dbda.domain.usecase

import android.util.Log
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.dbda.domain.usecase.analyser.UnsafeBehaviorAnalyser
import com.uoa.dbda.repository.UnsafeBehaviourRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class FetchRawSensorDataByDate @Inject constructor(
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository,
){
    suspend fun execute(startDate: Long, endDate: Long): Flow<List<RawSensorData>> {
        val sensorDataList =
            unsafeBehaviourRepository.getSensorDataBetweenDates(Date(startDate), Date(endDate))
        Log.i(
            "AnalyzeUnsafeBehaviorUseCase", "Number of sensor data: ${
                sensorDataList.collect {
                    it.size
                }
            }, startDate: ${Date(startDate)}, endDate: ${Date(endDate)}"
        )
        return sensorDataList.map { sensorDList ->
            sensorDList.map { it.toDomainModel() }
        }
    }
}