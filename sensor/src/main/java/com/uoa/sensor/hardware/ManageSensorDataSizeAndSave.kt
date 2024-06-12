package com.uoa.sensor.hardware

import android.util.Log
import com.uoa.sensor.data.model.RawSensorData
import com.uoa.sensor.data.repository.RawSensorDataRepository
import com.uoa.sensor.data.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ManageSensorDataSizeAndSave @Inject constructor(private val rawSensorDataRepository: RawSensorDataRepository){
       private val sensorDataBuffer = mutableListOf<RawSensorData>()

    fun addToSensorDataBuffer(rawSensorData: RawSensorData) {
        sensorDataBuffer.add(rawSensorData)
        if (sensorDataBuffer.size >= sensorDataBufferLimit) {
            processAndStoreSensorData()
        }
    }

    fun processAndStoreSensorData() {
        if (sensorDataBuffer.isEmpty()) return

        val bufferCopy = sensorDataBuffer.toList()
        sensorDataBuffer.clear()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rawSensorDataRepository.insertRawSensorDataBatch(bufferCopy.map { it.toEntity() })
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error processing and storing sensor data", e)
            }
        }
    }

    companion object {
        private const val sensorDataBufferLimit = 50  // Reduced buffer limit to manage memory usage
    }
}