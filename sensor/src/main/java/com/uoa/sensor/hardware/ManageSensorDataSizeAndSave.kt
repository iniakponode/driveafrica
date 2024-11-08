package com.uoa.sensor.hardware

import android.util.Log
import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.RawSensorData
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

    fun addToSensorDataBufferAndSave(rawSensorData: RawSensorData) {
        sensorDataBuffer.add(rawSensorData)
        if (sensorDataBuffer.size >= SENSORDATABUFFERLIMIT) {
            processSensorData()
//            analyseAndSaveRawData()
        }
    }

//    Process and store sensor data in the buffer as well as analyse same for unsafe driving behaviour and save the result
    fun processSensorData(): List<RawSensorDataEntity> {
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

//    fun analyseAndSaveRawData() {
//
//        processSensorData()
//    }

//    fun ananalyseAndSaveUnsafeBehaviours(sensorDataList: List<RawSensorDataEntity>){
//        val unsafeBehaviorAnalyser = UnsafeBehaviorAnalyser()
//        if (sensorDataList.isEmpty()) return
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                unsafeBehaviorAnalyser.analyse(sensorDataList).apply {
//                    if (isNotEmpty()) unsafeBehaviourRepository.insertUnsafeBehaviourBatch(this)
//                }
//            } catch (e: Exception) {
//                Log.e("HardwareModule", "Error analysing sensor data", e)
//            }
//        }
//    }

    companion object {
        private const val SENSORDATABUFFERLIMIT = 50  // Reduced buffer limit to manage memory usage
    }
}