package com.uoa.core.behaviouranalysis


import android.os.Build
import androidx.annotation.RequiresApi
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.RawSensorData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import java.util.UUID
import javax.inject.Inject
import kotlin.math.sqrt


class UnsafeBehaviorAnalyser {

    fun analyse(sensorDataList: List<RawSensorDataEntity>): List<UnsafeBehaviourModel> {
        val unsafeBehaviours = mutableListOf<UnsafeBehaviourModel>()

        // Filter accelerometer data
        val accelerometerData = sensorDataList.filter { it.sensorType == ACCELEROMETER_TYPE }

        // Filter speed data
        val speedData = sensorDataList.filter { it.sensorType == SPEED_TYPE }

        // Filter rotation vector data
        val rotationVectorData = sensorDataList.filter { it.sensorType == ROTATION_VECTOR_TYPE }

        // Analyze sensor data and detect unsafe behaviours using sliding window approach
        for (data in accelerometerData) {
            if (isHarshAcceleration(data)) {
                unsafeBehaviours.add(
                    UnsafeBehaviourModel(
                        id = UUID.randomUUID(),
                        tripId = data.tripId!!,
                        behaviorType = "Harsh Acceleration",
                        timestamp = data.timestamp,
                        date = data.date!!,
                        updatedAt = null,
                        updated = false,
                        severity = 1.0f,
                        locationId = data.locationId,
                    )
                )
            }

            if (isHarshBraking(data)) {
                unsafeBehaviours.add(
                    UnsafeBehaviourModel(
                        id = UUID.randomUUID(),
                        tripId = data.tripId!!,
                        behaviorType = "Harsh Braking",
                        timestamp = data.timestamp,
                        date = data.date!!,
                        updatedAt = null,
                        updated = false,
                        severity = 1.0f,
                        locationId = data.locationId,
                    )
                )
            }
        }

        // Detect speeding
        for (data in speedData) {
            if (isSpeeding(data)) {
                unsafeBehaviours.add(
                    UnsafeBehaviourModel(
                        id = UUID.randomUUID(),
                        tripId = data.tripId!!,
                        behaviorType = "Speeding",
                        timestamp = data.timestamp,
                        date = data.date!!,
                        updatedAt = null,
                        updated = false,
                        severity = 1.0f,
                        locationId = data.locationId,
                    )
                )
            }
        }

        // Detect swerving
        for (data in rotationVectorData) {
            if (isSwerving(data)) {
                unsafeBehaviours.add(
                    UnsafeBehaviourModel(
                        id = UUID.randomUUID(),
                        tripId = data.tripId!!,
                        behaviorType = "Swerving",
                        timestamp = data.timestamp,
                        date = data.date!!,
                        updatedAt = null,
                        updated = false,
                        severity = 1.0f,
                        locationId = data.locationId,
                    )
                )
            }
        }

        return unsafeBehaviours
    }

    private fun isHarshAcceleration(data: RawSensorDataEntity): Boolean {
        val accelerationMagnitude = calculateAccelerationMagnitude(data.values)
        return accelerationMagnitude > ACCELERATION_THRESHOLD
    }

    private fun isHarshBraking(data: RawSensorDataEntity): Boolean {
        val accelerationMagnitude = calculateAccelerationMagnitude(data.values)
        return accelerationMagnitude > BRAKING_THRESHOLD
    }

    private fun isSpeeding(data: RawSensorDataEntity): Boolean {
        val speed = data.values[0] // Assuming the first value is the speed
        return speed > SPEED_LIMIT
    }

    private fun isSwerving(data: RawSensorDataEntity): Boolean {
        val rotationMagnitude = calculateRotationMagnitude(data.values)
        return rotationMagnitude > SWERVING_THRESHOLD
    }

    private fun calculateAccelerationMagnitude(values: List<Float>): Float {
        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    private fun calculateRotationMagnitude(values: List<Float>): Float {
        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    companion object {
        const val ACCELEROMETER_TYPE = 1 // Assuming 1 is the sensorType for accelerometer
        const val SPEED_TYPE = 2 // Assuming 2 is the sensorType for speed
        const val ROTATION_VECTOR_TYPE = 11 // Assuming 11 is the sensorType for rotation vector
        const val ACCELERATION_THRESHOLD = 3.5 // Example threshold for harsh acceleration (m/s^2)
        const val BRAKING_THRESHOLD = -3.5 // Example threshold for harsh braking (m/s^2)
        const val SPEED_LIMIT = -30.0 // Example speed limit (m/s)
        const val SWERVING_THRESHOLD = 0.1 // Example threshold for swerving (rad/s)
    }
}
