package com.uoa.sensor.hardware

import android.util.Log
//import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageSensorDataSizeAndSave @Inject constructor(
    private val rawSensorDataRepository: RawSensorDataRepository,
//    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {

    /**
     * Persists the given list of sensor data entities in the database.
     *
     * @param sensorDataBatch The list of `RawSensorData` to be persisted.
     */
    fun saveSensorData(sensorDataBatch: List<RawSensorData>) {
        if (sensorDataBatch.isEmpty()) return

        // Launch a coroutine to persist data asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rawSensorDataRepository.insertRawSensorDataBatch(sensorDataBatch.map { it.toEntity() })
                Log.d("ManageSensorData", "Batch saved successfully: ${sensorDataBatch.size} entries")
            } catch (e: Exception) {
                Log.e("ManageSensorData", "Error saving sensor data batch", e)
            }
        }
    }

    /**
     * Analyzes sensor data to detect unsafe driving behaviors and save the result.
     *
     * @param sensorDataBatch The list of `RawSensorData` to be analyzed.
     */
//    fun analyzeAndSaveUnsafeBehaviour(sensorDataBatch: List<RawSensorData>) {
//        if (sensorDataBatch.isEmpty()) return
//
//        // Perform unsafe behavior analysis based on sensor data
//        CoroutineScope(Dispatchers.Default).launch {
//            try {
//                val unsafeBehaviours = analyzeSensorDataForUnsafeBehaviour(sensorDataBatch)
//                unsafeBehaviourRepository.insertUnsafeBehaviours(unsafeBehaviours)
//                Log.d("ManageSensorData", "Unsafe behaviour analysis completed and saved.")
//            } catch (e: Exception) {
//                Log.e("ManageSensorData", "Error analyzing and saving unsafe behaviour", e)
//            }
//        }
//    }

    /**
     * Analyzes sensor data to determine unsafe behavior patterns.
     *
     * @param sensorDataBatch List of `RawSensorData` to analyze.
     * @return List of unsafe behavior entities detected.
     */
//    private fun analyzeSensorDataForUnsafeBehaviour(sensorDataBatch: List<RawSensorData>): List<UnsafeBehaviourEntity> {
//        // Perform analysis logic here - example analysis process
//        return sensorDataBatch.filter { data ->
//            // Example: detecting harsh braking or sharp turn
//            data.values.any { it > UNSAFE_BEHAVIOUR_THRESHOLD }
//        }.map {
//            UnsafeBehaviourEntity(
//                id = UUID.randomUUID(),
//                sensorType = it.sensorType,
//                timestamp = it.timestamp,
//                tripId = it.tripId,
//                description = "Unsafe behaviour detected: ${it.sensorTypeName}"
//            )
//        }
//    }

    companion object {
        private const val UNSAFE_BEHAVIOUR_THRESHOLD = 10.0f // Example threshold for unsafe behaviour detection
    }
}
