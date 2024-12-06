package com.uoa.sensor.hardware

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorDataBufferManager @Inject constructor(
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {

    private val sensorDataBuffer = mutableListOf<RawSensorData>()
    private val bufferInsertInterval: Long = 5000  // Time interval to process the buffer (e.g., every 5 seconds)
    private val bufferLimit = 100  // Threshold limit for batch size

    private val bufferHandler = Handler(Looper.getMainLooper())

    init {
        startBufferFlushHandler()
    }

    // Add sensor data to the buffer
    fun addToSensorBuffer(rawSensorData: RawSensorData) {
        synchronized(sensorDataBuffer) {
            sensorDataBuffer.add(rawSensorData)
//            Log.d("BufferManager", "Successfully added new data to the buffer ${sensorDataBuffer.last()}.")
            if (sensorDataBuffer.size >= bufferLimit) {
                processAndStoreSensorData()
            }
        }
    }

    // This method should flush the data periodically even if the buffer limit is not reached
    private fun startBufferFlushHandler() {
        bufferHandler.postDelayed(object : Runnable {
            override fun run() {
                processAndStoreSensorData()
                bufferHandler.postDelayed(this, bufferInsertInterval)
            }
        }, bufferInsertInterval)
    }

    // Process the data in the buffer
    fun processAndStoreSensorData() {
        val bufferCopy: List<RawSensorData>
        synchronized(sensorDataBuffer) {
            if (sensorDataBuffer.isEmpty()) return
            bufferCopy = sensorDataBuffer.toList()
            sensorDataBuffer.clear()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Batch insert sensor data into the repository
                rawSensorDataRepository.insertRawSensorDataBatch(bufferCopy.map { it.toEntity() })
//                Log.d("BufferManager", "Successfully stored sensor data batch.")

                // Analyze the data
                val analyzer = NewUnsafeDrivingBehaviourAnalyser()
                val sensorDataFlow = bufferCopy.asFlow().map { it.toEntity() }
                analyzer.analyze(sensorDataFlow)
                    .collect { unsafeBehaviour ->
                        // Handle the detected unsafe behavior, e.g., store into database
                        unsafeBehaviourRepository.insertUnsafeBehaviour(unsafeBehaviour)
                    }

            } catch (e: Exception) {
                Log.e("BufferManager", "Error storing sensor data", e)
            }
        }
    }


    // This could be used to stop any background buffer flushing if needed
    fun clear() {
        bufferHandler.removeCallbacksAndMessages(null)
    }
}