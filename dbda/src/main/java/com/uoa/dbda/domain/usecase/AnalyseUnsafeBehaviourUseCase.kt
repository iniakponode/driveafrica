package com.uoa.dbda.domain.usecase.analyser


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.dbda.repository.UnsafeBehaviourRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class AnalyzeUnsafeBehaviorUseCase @Inject constructor(
    private val unsafeBehaviorAnalyser: UnsafeBehaviorAnalyser,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository,
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun execute(startDate: Long, endDate: Long) {
        val sensorDataList = unsafeBehaviourRepository.getSensorDataBetweenDates(Date(startDate), Date(endDate))
        Log.i("AnalyzeUnsafeBehaviorUseCase", "Number of sensor data: ${sensorDataList.collect{
            it.size
        }}, startDate: ${Date(startDate)}, endDate: ${Date(endDate)}")

        sensorDataList.collect{ sensorDataEntities ->
            val unsafeBehaviours = unsafeBehaviorAnalyser.analyse(sensorDataEntities)
            Log.i("AnalyzeUnsafeBehaviorUseCase", "Number of unsafe behaviours: ${unsafeBehaviours.size}")
            unsafeBehaviourRepository.insertUnsafeBehaviourBatch(unsafeBehaviours)
            }
        }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun analyseByTrip(tripId: UUID) {
        val sensorDataList = unsafeBehaviourRepository.getSensorDataByTripId(tripId)

        sensorDataList.collect{ sensorDataEntities ->
            val unsafeBehaviours = unsafeBehaviorAnalyser.analyse(sensorDataEntities)
            unsafeBehaviourRepository.insertUnsafeBehaviourBatch(unsafeBehaviours)
            }
        }

    
}
