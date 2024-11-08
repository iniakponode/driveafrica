//package com.uoa.dbda.domain.usecase.analyser
//
//
//import android.os.Build
//import androidx.annotation.RequiresApi
//import com.uoa.core.database.entities.RawSensorDataEntity
//import com.uoa.core.model.RawSensorData
//import com.uoa.core.model.UnsafeBehaviourModel
//import com.uoa.core.utils.toDomainModel
//import java.util.UUID
//import javax.inject.Inject
//import kotlin.math.sqrt
//class UnsafeBehaviorAnalyser @Inject constructor() {
//
//    fun analyse(sensorDataList: List<RawSensorDataEntity>): List<UnsafeBehaviourModel> {
//        val unsafeBehaviours = mutableListOf<UnsafeBehaviourModel>()
//
//        // Filter accelerometer data
//        val accelerometerData = sensorDataList.filter { it.sensorType == ACCELEROMETER_TYPE }
//
//        // Analyze sensor data and detect unsafe behaviours using sliding window approach
////        val smoothedData = smoothData(accelerometerData)
//
//        for (data in accelerometerData) {
//            if (isHarshAcceleration(data)) {
//                unsafeBehaviours.add(
//                    UnsafeBehaviourModel(
//                        id = UUID.randomUUID(),
//                        tripId = data.tripId!!,
//                        behaviorType = "Harsh Acceleration",
//                        timestamp = data.timestamp,
//                        date = data.date!!,
//                        updatedAt = null,
//                        updated=false,
//                        severity = 1.0f,
//                        locationId = data.locationId,
//                    )
//                )
//            }
//
//            if (isHarshBraking(data)) {
//                unsafeBehaviours.add(
//                    UnsafeBehaviourModel(
//                        id = UUID.randomUUID(),
//                        tripId = data.tripId!!,
//                        behaviorType = "Harsh Braking",
//                        timestamp = data.timestamp,
//                        date = data.date!!,
//                        updatedAt = null,
//                        updated=false,
//                        severity = 1.0f,
//                        locationId = data.locationId,
//                    )
//                )
//            }
//        }
//
//        return unsafeBehaviours
//    }
//
//    private fun isHarshAcceleration(data: RawSensorDataEntity): Boolean {
//        // Replace with your actual harsh acceleration detection logic
//        val accelerationMagnitude = calculateAccelerationMagnitude(data.values)
//        return accelerationMagnitude > ACCELERATION_THRESHOLD
//    }
//
//    private fun isHarshBraking(data: RawSensorDataEntity): Boolean {
//        val accelerationMagnitude = calculateAccelerationMagnitude(data.values)
//        return accelerationMagnitude < BRAKING_THRESHOLD
//    }
//
//    private fun calculateAccelerationMagnitude(values: List<Float>): Float {
//        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
//    }
//
//    private fun smoothData(data: List<RawSensorDataEntity>): List<RawSensorDataEntity> {
//        val smoothedData = mutableListOf<RawSensorDataEntity>()
//        val windowSize = WINDOW_SIZE / 2 // Half window size for smoothing
//
//        for (i in data.indices) {
//            val start = maxOf(0, i - windowSize)
//            val end = minOf(data.size - 1, i + windowSize)
//            val window = data.subList(start, end + 1)
//            val avgValues = window.map { it.values }
//                .reduce { acc, list ->
//                    listOf(
//                        acc[0] + list[0],
//                        acc[1] + list[1],
//                        acc[2] + list[2]
//                    )
//                }.map { it / window.size }
//
//            smoothedData.add(
//                data[i].copy(
//                    values = avgValues
//                )
//            )
//        }
//        return smoothedData
//    }
//
//    companion object {
//        const val ACCELEROMETER_TYPE = 1 // Assuming 1 is the sensorType for accelerometer
//        const val ACCELERATION_THRESHOLD = 0.9 // Example threshold for harsh acceleration (m/s^2)
//        const val BRAKING_THRESHOLD = -0.9 // Example threshold for harsh braking (m/s^2)
//        const val WINDOW_SIZE = 200 // Assuming 200 samples per second
//    }
//}
