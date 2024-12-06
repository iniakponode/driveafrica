package com.uoa.core.behaviouranalysis

import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.UnsafeBehaviourModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class NewUnsafeDrivingBehaviourAnalyser {
    private val accelerometerWindow = ArrayDeque<RawSensorDataEntity>()
    private val rotationWindow = ArrayDeque<RawSensorDataEntity>()
    private val speedWindow = ArrayDeque<RawSensorDataEntity>()
    private val windowSize = 100  // Adjust based on desired sensitivity and resource constraints

    fun analyze(sensorDataFlow: Flow<RawSensorDataEntity>): Flow<UnsafeBehaviourModel> = flow {
        sensorDataFlow.collect { data ->
            when (data.sensorType) {
                ACCELEROMETER_TYPE -> {
                    accelerometerWindow.addLast(data)
                    if (accelerometerWindow.size > windowSize) accelerometerWindow.removeFirst()
                    val behavior = analyzeAccelerometerData()
                    if (behavior != null) emit(behavior)
                }
                ROTATION_VECTOR_TYPE -> {
                    rotationWindow.addLast(data)
                    if (rotationWindow.size > windowSize) rotationWindow.removeFirst()
                    val behavior = analyzeRotationData()
                    if (behavior != null) emit(behavior)
                }
                SPEED_TYPE -> {
                    speedWindow.addLast(data)
                    if (speedWindow.size > windowSize) speedWindow.removeFirst()
                    val behavior = analyzeSpeedData()
                    if (behavior != null) emit(behavior)
                }
            }
        }
    }

    private fun analyzeAccelerometerData(): UnsafeBehaviourModel? {
        val rms = sqrt(accelerometerWindow.map { calculateAccelerationMagnitude(it.values).pow(2) }.average())
        return when {
            rms > ACCELERATION_THRESHOLD -> {
                createUnsafeBehavior("Harsh Acceleration", accelerometerWindow.last(), rms.toFloat())
            }
            rms < BRAKING_THRESHOLD -> {
                createUnsafeBehavior("Harsh Braking", accelerometerWindow.last(), rms.toFloat())
            }
            else -> null
        }
    }

    private fun analyzeRotationData(): UnsafeBehaviourModel? {
        val rms = sqrt(rotationWindow.map { calculateRotationMagnitude(it.values).pow(2) }.average())
        return if (rms > SWERVING_THRESHOLD) {
            createUnsafeBehavior("Swerving", rotationWindow.last(), rms.toFloat())
        } else {
            null
        }
    }

    private fun analyzeSpeedData(): UnsafeBehaviourModel? {
        val averageSpeed = speedWindow.map { it.values[0] }.average()
        return if (averageSpeed > SPEED_LIMIT) {
            createUnsafeBehavior("Speeding", speedWindow.last(), averageSpeed.toFloat())
        } else {
            null
        }
    }

    private fun createUnsafeBehavior(
        behaviorType: String,
        data: RawSensorDataEntity,
        measuredValue: Float
    ): UnsafeBehaviourModel {
        val severity = calculateSeverity(behaviorType, measuredValue)
        return UnsafeBehaviourModel(
            id = UUID.randomUUID(),
            tripId = data.tripId!!,
            behaviorType = behaviorType,
            timestamp = data.timestamp,
            date = data.date!!,
            updatedAt = null,
            updated = false,
            severity = severity,
            locationId = data.locationId,
        )
    }

    private fun calculateSeverity(behaviorType: String, measuredValue: Float): Float {
        return when (behaviorType) {
            "Harsh Acceleration" -> {
                val excess = measuredValue - ACCELERATION_THRESHOLD
                (excess / MAX_ACCELERATION_EXCESS).coerceIn(0f, 1f)
            }
            "Harsh Braking" -> {
                val excess = abs(measuredValue) - abs(BRAKING_THRESHOLD)
                (excess / MAX_BRAKING_EXCESS).coerceIn(0f, 1f)
            }
            "Swerving" -> {
                val excess = measuredValue - SWERVING_THRESHOLD
                (excess / MAX_SWERVING_EXCESS).coerceIn(0f, 1f)
            }
            "Speeding" -> {
                val excess = measuredValue - SPEED_LIMIT
                (excess / MAX_SPEED_EXCESS).coerceIn(0f, 1f)
            }
            else -> 0f
        }
    }

    private fun calculateAccelerationMagnitude(values: List<Float>): Float {
        return sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
    }

    private fun calculateRotationMagnitude(values: List<Float>): Float {
        return sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
    }

    companion object {
        const val ACCELEROMETER_TYPE = 1
        const val SPEED_TYPE = 2
        const val ROTATION_VECTOR_TYPE = 11

        // Thresholds
        const val ACCELERATION_THRESHOLD = 3.5f  // m/s²
        const val BRAKING_THRESHOLD = -3.5f      // m/s²
        const val SWERVING_THRESHOLD = 0.1f      // rad/s
        const val SPEED_LIMIT = 30.0f            // m/s (~108 km/h)

        // Maximum expected excesses (for normalization)
        const val MAX_ACCELERATION_EXCESS = 6.5f   // m/s² (e.g., up to 10 m/s²)
        const val MAX_BRAKING_EXCESS = 6.5f        // m/s² (e.g., down to -10 m/s²)
        const val MAX_SWERVING_EXCESS = 0.9f       // rad/s (e.g., up to 1 rad/s)
        const val MAX_SPEED_EXCESS = 20.0f         // m/s (e.g., up to 50 m/s total speed)
    }
}