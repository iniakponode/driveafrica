package com.uoa.core.behaviouranalysis

import android.content.Context
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.PreferenceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class NewUnsafeDrivingBehaviourAnalyser @Inject constructor(
    private val locationRepository: LocationRepository // Injected repository
) {

    private val accelerometerWindow = ArrayDeque<RawSensorDataEntity>()
    private val rotationWindow = ArrayDeque<RawSensorDataEntity>()
    private val speedWindow = ArrayDeque<RawSensorDataEntity>()
    private val windowSize = 100  // Adjust based on desired sensitivity and resource constraints

    fun analyze(sensorDataFlow: Flow<RawSensorDataEntity>, context: Context): Flow<UnsafeBehaviourModel> = flow {
        sensorDataFlow.collect { data ->
            when (data.sensorType) {
                ACCELEROMETER_TYPE -> {
                    accelerometerWindow.addLast(data)
                    if (accelerometerWindow.size > windowSize) accelerometerWindow.removeFirst()
                    val behavior = analyzeAccelerometerData(context)
                    if (behavior != null) emit(behavior)
                }
                ROTATION_VECTOR_TYPE -> {
                    rotationWindow.addLast(data)
                    if (rotationWindow.size > windowSize) rotationWindow.removeFirst()
                    val behavior = analyzeRotationData(context)
                    if (behavior != null) emit(behavior)
                }
                SPEED_TYPE -> {
                    speedWindow.addLast(data)
                    if (speedWindow.size > windowSize) speedWindow.removeFirst()
                    val behavior = analyzeSpeedData(context)
                    if (behavior != null) emit(behavior)
                }
            }
        }
    }

    private fun analyzeAccelerometerData(context: Context): UnsafeBehaviourModel? {
        val rms = sqrt(accelerometerWindow.map { calculateAccelerationMagnitude(it.values).pow(2) }.average())
        return when {
            rms > ACCELERATION_THRESHOLD -> {
                createUnsafeBehavior("Harsh Acceleration", accelerometerWindow.last(), rms.toFloat(), context)
            }
            rms < BRAKING_THRESHOLD -> {
                createUnsafeBehavior("Harsh Braking", accelerometerWindow.last(), rms.toFloat(), context)
            }
            else -> null
        }
    }

    private fun analyzeRotationData(context: Context): UnsafeBehaviourModel? {
        val rms = sqrt(rotationWindow.map { calculateRotationMagnitude(it.values).pow(2) }.average())
        return if (rms > SWERVING_THRESHOLD) {
            createUnsafeBehavior("Swerving", rotationWindow.last(), rms.toFloat(), context)
        } else {
            null
        }
    }

    private suspend fun analyzeSpeedData(context: Context): UnsafeBehaviourModel? {
        val speedValues = speedWindow.mapNotNull { data ->
            data.locationId?.let { locationId ->
                fetchLocationDataById(locationId)?.speed
            } ?: run {
                if (data.sensorType == ACCELEROMETER_TYPE) calculateSpeedFromAccelerometer(data.values) else null
            }
        }

// Step 1: Extract a representative locationId from the speedWindow (e.g., the first non-null one)
        val firstLocationId = speedWindow.firstNotNullOfOrNull { it.locationId }

        if (speedValues.isNotEmpty() && firstLocationId != null) {
            val averageSpeed = speedValues.average()

            // Step 2: Fetch location data using the representative locationId
            val locationData = fetchLocationDataById(firstLocationId)

            // Step 3: Determine effective speed limit as Double
            val effectiveSpeedLimit: Double = if (locationData?.speedLimit != null && locationData.speedLimit.toInt() != 0) {
                locationData.speedLimit.toDouble()
            } else {
                SPEED_LIMIT.toDouble()
            }

            // Step 4: Compare average speed to the effective speed limit
            return if (averageSpeed > effectiveSpeedLimit) {
                createUnsafeBehavior("Speeding", speedWindow.last(), averageSpeed.toFloat(), context)
            } else {
                null
            }
        }

        return null


        return null
    }

    private suspend fun fetchLocationDataById(locationId: UUID): LocationData? {
        val locationEntity = locationRepository.getLocationById(locationId)
        return locationEntity?.let {
            LocationData(
                id = it.id,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                speed = it.speed.toDouble(),
                distance = it.distance.toDouble(),
                timestamp = it.timestamp,
                date = it.date,
                speedLimit = it.speedLimit,
                processed = it.processed,
                sync = it.sync
            )
        }
    }

    private fun createUnsafeBehavior(
        behaviorType: String,
        data: RawSensorDataEntity,
        measuredValue: Float,
        context: Context
    ): UnsafeBehaviourModel {
        val severity = calculateSeverity(behaviorType, measuredValue)
        return UnsafeBehaviourModel(
            id = UUID.randomUUID(),
            tripId = data.tripId!!,
            driverProfileId = PreferenceUtils.getDriverProfileId(context)!!,
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

    private fun calculateSpeedFromAccelerometer(values: List<Float>): Double? {
        if (values.size < 3) return null
        val magnitude = calculateAccelerationMagnitude(values)
        val timeInterval = 0.1 // Approximate interval (seconds) between accelerometer readings
        return magnitude * timeInterval
    }

    companion object {
        const val ACCELEROMETER_TYPE = 1
        const val SPEED_TYPE = 2 // Magnetometer or fallback logic
        const val ROTATION_VECTOR_TYPE = 11

        const val ACCELERATION_THRESHOLD = 3.5f  // m/s²
        const val BRAKING_THRESHOLD = -3.5f      // m/s²
        const val SWERVING_THRESHOLD = 0.1f      // rad/s
        const val SPEED_LIMIT = 27.78f // m/s (~100 km/h)

        const val MAX_ACCELERATION_EXCESS = 6.5f
        const val MAX_BRAKING_EXCESS = 6.5f
        const val MAX_SWERVING_EXCESS = 0.9f
        const val MAX_SPEED_EXCESS = 2.778f
    }
}