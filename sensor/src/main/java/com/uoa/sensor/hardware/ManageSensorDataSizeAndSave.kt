package com.uoa.sensor.hardware

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.RawSensorData
import com.uoa.sensor.repository.RawSensorDataRepositoryImpl
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ManageSensorDataSizeAndSave @Inject constructor(
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
    ){
       private val sensorDataBuffer = mutableListOf<RawSensorData>()

    @RequiresApi(Build.VERSION_CODES.O)
    fun addToSensorDataBuffer(rawSensorData: RawSensorData) {
        sensorDataBuffer.add(rawSensorData)
        if (sensorDataBuffer.size >= sensorDataBufferLimit) {
//            processAndStoreSensorData()
            analyseAndSaveRawSensorAndUnsafeBehavioursData()
        }
    }

//    Process and store sensor data in the buffer as well as analyse same for unsafe driving behaviour and save the result
    fun processAndStoreSensorData(): List<RawSensorDataEntity> {
        if (sensorDataBuffer.isEmpty()) return emptyList()

        val bufferCopy = sensorDataBuffer.toList()
        sensorDataBuffer.clear()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rawSensorDataRepository.insertRawSensorDataBatch(bufferCopy.map { it.toEntity() })
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error processing and storing sensor data", e)
            }
        }
        return bufferCopy.map { it.toEntity() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun analyseAndSaveRawSensorAndUnsafeBehavioursData() {
        val unsafeBehaviorAnalyser = UnsafeBehaviorAnalyser()
        val sensorDataList= processAndStoreSensorData()
        if (sensorDataList.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                unsafeBehaviorAnalyser.analyse(sensorDataList).apply {
                    if (isNotEmpty()) unsafeBehaviourRepository.insertUnsafeBehaviourBatch(this)
                }
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error analysing sensor data", e)
            }
        }
    }

    companion object {
        private const val sensorDataBufferLimit = 50  // Reduced buffer limit to manage memory usage
    }
}