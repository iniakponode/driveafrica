package com.uoa.core.behaviouranalysis

import android.content.Context
import android.util.Log
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.PreferenceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

//
//New Code

@Singleton
class NewUnsafeDrivingBehaviourAnalyser @Inject constructor(
    private val locationRepository: LocationRepository
) {

    private data class AnalysisWindow(
        val accelerometerWindow: ArrayDeque<RawSensorDataEntity> = ArrayDeque(),
        val rotationWindow: ArrayDeque<RawSensorDataEntity> = ArrayDeque(),
        val speedWindow: ArrayDeque<RawSensorDataEntity> = ArrayDeque(),
        var hasLinearAcceleration: Boolean = false,
        var lastSpeedAnalysisTimeMs: Long = 0L,
        var accelCandidateStartMs: Long = 0L,
        var brakeCandidateStartMs: Long = 0L,
        var lastAccelerationEventMs: Long = 0L,
        var lastBrakingEventMs: Long = 0L,
        var speedingStartMs: Long = 0L,
        var lastSpeedingEventMs: Long = 0L,
        var lastSwervingEventMs: Long = 0L
    )

    // Maximum number of sensor events to hold in each window.
    private val windowSize = 100
    private val speedAnalysisIntervalMs = 1_000L

    /**
     * Processes a Flow of raw sensor data in real time and emits unsafe behavior events.
     *
     * For each sensor event, we update the appropriate sliding window and call a helper function
     * to detect unsafe behavior. If a behavior is detected, it is immediately emitted downstream.
     */
    fun analyze(
        sensorDataFlow: Flow<RawSensorDataEntity>,
        context: Context
    ): Flow<UnsafeBehaviourModel> = flow {
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Starting analysis of sensor data flow")
        val analysisWindow = AnalysisWindow()
        sensorDataFlow.collect { data ->
            processSensorData(data, context, analysisWindow)?.let { behavior ->
                emit(behavior)
            }
        }
    }

    /**
     * Processes a single sensor data event by updating its window and calling the appropriate analysis.
     */
    private suspend fun processSensorData(
        data: RawSensorDataEntity,
        context: Context,
        window: AnalysisWindow
    ): UnsafeBehaviourModel? {
        Log.d(
            "NewUnsafeDrivingBehaviourAnalyser",
            "Processing sensor data: sensorType=${data.sensorType}, timestamp=${data.timestamp}"
        )
        var behavior: UnsafeBehaviourModel? = when (data.sensorType) {
            LINEAR_ACCELERATION_TYPE -> {
                window.hasLinearAcceleration = true
                addToWindow(window.accelerometerWindow, data)
                analyzeAccelerometerData(window, context)
            }
            ACCELEROMETER_TYPE -> {
                if (!window.hasLinearAcceleration) {
                    addToWindow(window.accelerometerWindow, data)
                    analyzeAccelerometerData(window, context)
                } else {
                    null
                }
            }
            ROTATION_VECTOR_TYPE -> {
                addToWindow(window.rotationWindow, data)
                analyzeRotationData(window, context)
            }
            GYROSCOPE_TYPE -> {
                addToWindow(window.rotationWindow, data)
                analyzeRotationData(window, context)
            }
            else -> null
        }

        if (behavior != null) return behavior

        if (data.locationId != null) {
            addToWindow(window.speedWindow, data)
            if (shouldAnalyzeSpeed(data.timestamp, window)) {
                behavior = analyzeSpeedData(window, context)
            }
        }

        if (behavior == null && data.locationId == null && data.sensorType != ACCELEROMETER_TYPE &&
            data.sensorType != LINEAR_ACCELERATION_TYPE && data.sensorType != ROTATION_VECTOR_TYPE &&
            data.sensorType != GYROSCOPE_TYPE
        ) {
            Log.w("NewUnsafeDrivingBehaviourAnalyser", "Skipping unsupported sensor type: ${data.sensorType}")
        }

        return behavior
    }

    private fun addToWindow(window: ArrayDeque<RawSensorDataEntity>, data: RawSensorDataEntity) {
        window.addLast(data)
        if (window.size > windowSize) {
            window.removeFirst()
        }
    }

    private fun shouldAnalyzeSpeed(timestamp: Long, window: AnalysisWindow): Boolean {
        val last = window.lastSpeedAnalysisTimeMs
        val shouldAnalyze = last == 0L || timestamp - last >= speedAnalysisIntervalMs
        if (shouldAnalyze) {
            window.lastSpeedAnalysisTimeMs = timestamp
        }
        return shouldAnalyze
    }

    private fun estimateSignedAcceleration(accelWindow: ArrayDeque<RawSensorDataEntity>): Double {
        if (accelWindow.isEmpty()) return 0.0
        val sampleCount = minOf(5, accelWindow.size)
        val samples = accelWindow.takeLast(sampleCount)
        val sums = DoubleArray(3)
        var count = 0
        for (sample in samples) {
            if (sample.values.size < 3) continue
            sums[0] += sample.values[0]
            sums[1] += sample.values[1]
            sums[2] += sample.values[2]
            count++
        }
        if (count == 0) return 0.0
        val avgX = sums[0] / count
        val avgY = sums[1] / count
        val absX = abs(avgX)
        val absY = abs(avgY)
        return if (absX >= absY) avgX else avgY
    }

    private fun recentSamples(
        window: ArrayDeque<RawSensorDataEntity>,
        durationMs: Long
    ): List<RawSensorDataEntity> {
        return recentSamples(window.toList(), durationMs)
    }

    private fun recentSamples(
        samples: List<RawSensorDataEntity>,
        durationMs: Long
    ): List<RawSensorDataEntity> {
        val last = samples.lastOrNull() ?: return emptyList()
        val cutoff = last.timestamp - durationMs
        return samples.filter { it.timestamp >= cutoff }
    }

    private fun computeStdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    private fun computeZStdDev(samples: List<RawSensorDataEntity>): Double {
        val zValues = samples.mapNotNull { it.values.getOrNull(2)?.toDouble() }
        return computeStdDev(zValues)
    }

    private fun computeZPeakToPeak(samples: List<RawSensorDataEntity>): Double {
        val zValues = samples.mapNotNull { it.values.getOrNull(2)?.toDouble() }
        if (zValues.isEmpty()) return 0.0
        val max = zValues.maxOrNull() ?: 0.0
        val min = zValues.minOrNull() ?: 0.0
        return max - min
    }

    private fun isPotholeLike(samples: List<RawSensorDataEntity>): Boolean {
        val zStdDev = computeZStdDev(samples)
        val zPeakToPeak = computeZPeakToPeak(samples)
        return zStdDev > POTHOLE_Z_STDDEV_THRESHOLD || zPeakToPeak > POTHOLE_Z_PEAK_TO_PEAK
    }

    private fun computeWindowDurationMs(samples: List<RawSensorDataEntity>): Long {
        if (samples.size < 2) return 0L
        return samples.last().timestamp - samples.first().timestamp
    }

    private fun computeMaxYawRate(samples: List<RawSensorDataEntity>): Double {
        val rates = samples.mapNotNull { it.values.getOrNull(2)?.let { value -> abs(value).toDouble() } }
        return rates.maxOrNull() ?: 0.0
    }

    // -------------------------------------------------------------------------------
    // Accelerometer Analysis
    // -------------------------------------------------------------------------------
    private suspend fun analyzeAccelerometerData(
        window: AnalysisWindow,
        context: Context
    ): UnsafeBehaviourModel? {
        val accelWindow = window.accelerometerWindow
        if (accelWindow.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer window is empty, skipping analysis")
            return null
        }
        val rms = sqrt(accelWindow.map { calculateAccelerationMagnitude(it.values).pow(2) }.average())
        val now = accelWindow.last().timestamp
        val currentSpeedMps = estimateCurrentSpeedMps(window)
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Current speed (m/s): $currentSpeedMps")
        if (currentSpeedMps < MIN_ACCELERATION_SPEED_MPS) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speed under threshold. Skipping harsh accel/braking detection.")
            window.accelCandidateStartMs = 0L
            window.brakeCandidateStartMs = 0L
            return null
        }
        val speedAdjustment = dynamicThresholdAdjustment(currentSpeedMps)
        val roughnessSamples = recentSamples(accelWindow, ROUGHNESS_WINDOW_MS)
        val roughness = computeZStdDev(roughnessSamples)
        val roughnessAdjustment = (roughness * ROUGHNESS_MULTIPLIER).toFloat()
            .coerceAtMost(MAX_ROUGHNESS_ADJUSTMENT)
        val dynamicAccThreshold = (ACCELERATION_THRESHOLD - speedAdjustment + roughnessAdjustment)
            .coerceAtLeast(MIN_ACCELERATION_THRESHOLD)
        val dynamicBrakeThreshold = (kotlin.math.abs(BRAKING_THRESHOLD) - speedAdjustment + roughnessAdjustment)
            .coerceAtLeast(MIN_BRAKING_THRESHOLD)
        val signedAccel = estimateSignedAcceleration(accelWindow)
        val supportsBraking = window.hasLinearAcceleration

        return when {
            signedAccel > dynamicAccThreshold -> {
                if (window.accelCandidateStartMs == 0L) {
                    window.accelCandidateStartMs = now
                }
                val elapsed = now - window.accelCandidateStartMs
                if (elapsed >= ACCELERATION_MIN_DURATION_MS &&
                    now - window.lastAccelerationEventMs >= EVENT_COOLDOWN_MS
                ) {
                    Log.d("NewUnsafeDrivingBehaviourAnalyser", "Signed accel $signedAccel > $dynamicAccThreshold => Harsh Acceleration")
                    window.accelCandidateStartMs = 0L
                    window.lastAccelerationEventMs = now
                    createUnsafeBehavior("Harsh Acceleration", accelWindow.last(), signedAccel.toFloat(), context)
                } else {
                    null
                }
            }
            supportsBraking && signedAccel < -dynamicBrakeThreshold -> {
                val potholeSamples = recentSamples(accelWindow, POTHOLE_WINDOW_MS)
                if (isPotholeLike(potholeSamples)) {
                    window.brakeCandidateStartMs = 0L
                    Log.d("NewUnsafeDrivingBehaviourAnalyser", "Braking spike vetoed as road anomaly (pothole signature).")
                    return null
                }
                if (window.brakeCandidateStartMs == 0L) {
                    window.brakeCandidateStartMs = now
                }
                val elapsed = now - window.brakeCandidateStartMs
                if (elapsed >= ACCELERATION_MIN_DURATION_MS &&
                    now - window.lastBrakingEventMs >= EVENT_COOLDOWN_MS
                ) {
                    Log.d("NewUnsafeDrivingBehaviourAnalyser", "Signed accel $signedAccel < -$dynamicBrakeThreshold => Harsh Braking")
                    window.brakeCandidateStartMs = 0L
                    window.lastBrakingEventMs = now
                    createUnsafeBehavior("Harsh Braking", accelWindow.last(), kotlin.math.abs(signedAccel).toFloat(), context)
                } else {
                    null
                }
            }
            !supportsBraking && rms > dynamicAccThreshold -> {
                if (window.accelCandidateStartMs == 0L) {
                    window.accelCandidateStartMs = now
                }
                val elapsed = now - window.accelCandidateStartMs
                if (elapsed >= ACCELERATION_MIN_DURATION_MS &&
                    now - window.lastAccelerationEventMs >= EVENT_COOLDOWN_MS
                ) {
                    Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms > $dynamicAccThreshold => Harsh Acceleration (fallback)")
                    window.accelCandidateStartMs = 0L
                    window.lastAccelerationEventMs = now
                    createUnsafeBehavior("Harsh Acceleration", accelWindow.last(), rms.toFloat(), context)
                } else {
                    null
                }
            }
            else -> {
                window.accelCandidateStartMs = 0L
                window.brakeCandidateStartMs = 0L
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Acceleration within safe limits")
                null
            }
        }
    }

    // -------------------------------------------------------------------------------
    // Rotation Analysis
    // -------------------------------------------------------------------------------
    private suspend fun analyzeRotationData(
        window: AnalysisWindow,
        context: Context
    ): UnsafeBehaviourModel? {
        val rotationWindow = window.rotationWindow
        if (rotationWindow.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation window is empty, skipping analysis")
            return null
        }
        val currentSpeedMps = estimateCurrentSpeedMps(window)
        if (currentSpeedMps < MIN_SWERVE_SPEED_MPS) {
            return null
        }
        val now = rotationWindow.last().timestamp
        if (now - window.lastSwervingEventMs < SWERVE_COOLDOWN_MS) {
            return null
        }
        val gyroSamples = rotationWindow.filter { it.sensorType == GYROSCOPE_TYPE }
        val recentSamples = if (gyroSamples.isNotEmpty()) {
            recentSamples(gyroSamples, SWERVE_MAX_DURATION_MS)
        } else {
            recentSamples(rotationWindow, SWERVE_MAX_DURATION_MS)
        }
        val durationMs = computeWindowDurationMs(recentSamples)
        val yawRate = if (gyroSamples.isNotEmpty()) {
            computeMaxYawRate(recentSamples)
        } else {
            sqrt(recentSamples.map { calculateRotationMagnitude(it.values).pow(2) }.average())
        }
        return if (yawRate > SWERVING_THRESHOLD && durationMs <= SWERVE_MAX_DURATION_MS) {
            window.lastSwervingEventMs = now
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Yaw rate $yawRate > $SWERVING_THRESHOLD => Swerving")
            createUnsafeBehavior("Swerving", rotationWindow.last(), yawRate.toFloat(), context)
        } else {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation within safe limits")
            null
        }
    }

    // -------------------------------------------------------------------------------
    // Speed Analysis
    // -------------------------------------------------------------------------------
    private suspend fun analyzeSpeedData(
        window: AnalysisWindow,
        context: Context
    ): UnsafeBehaviourModel? {
        val speedWindow = window.speedWindow
        if (speedWindow.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speed window is empty, skipping analysis")
            return null
        }
        val (avgSpeed, locationData) = computeAverageSpeedAndLocation(speedWindow, window.accelerometerWindow)
        if (avgSpeed == 0.0) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Computed average speed is 0.0; skipping analysis")
            return null
        }
        val effectiveSpeedLimit: Double = locationData?.speedLimit?.takeIf { it.toInt() != 0 }
            ?.toDouble() ?: SPEED_LIMIT.toDouble()
        val overspeedThreshold = effectiveSpeedLimit * SPEED_TOLERANCE_RATIO
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Effective speed limit: $effectiveSpeedLimit, avgSpeed=$avgSpeed")
        val now = speedWindow.last().timestamp
        return if (avgSpeed > overspeedThreshold) {
            if (window.speedingStartMs == 0L) {
                window.speedingStartMs = now
            }
            val elapsed = now - window.speedingStartMs
            if (elapsed >= SPEEDING_MIN_DURATION_MS &&
                now - window.lastSpeedingEventMs >= SPEEDING_COOLDOWN_MS
            ) {
                window.lastSpeedingEventMs = now
                window.speedingStartMs = 0L
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speeding detected")
                createUnsafeBehavior(
                    "Speeding",
                    speedWindow.last(),
                    avgSpeed.toFloat(),
                    context,
                    overspeedThreshold.toFloat()
                )
            } else {
                null
            }
        } else {
            window.speedingStartMs = 0L
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "No speeding detected")
            null
        }
    }

    /**
     * Computes an average speed from the speedWindow.
     *
     * First, it gathers location-based speeds from the speedWindow.
     * If these are absent or average to 0, it falls back to computing speeds from the accelerometerWindow.
     *
     * It also returns a representative LocationData (if available) for determining the effective speed limit.
     */
    private suspend fun computeAverageSpeedAndLocation(
        speedWindow: ArrayDeque<RawSensorDataEntity>,
        accelWindow: ArrayDeque<RawSensorDataEntity>
    ): Pair<Double, LocationData?> {
        Log.d("SpeedCalculation", "Speed window size: ${speedWindow.size}")

        val locationCache = mutableMapOf<UUID, LocationData>()
        val locationSpeeds = mutableListOf<Double>()
        for (data in speedWindow) {
            val locationId = data.locationId ?: continue
            val locationData = locationCache[locationId] ?: fetchLocationDataById(locationId)?.also {
                locationCache[locationId] = it
            }
            val speed = locationData?.speed
            if (speed != null && speed > 0) {
                locationSpeeds.add(speed)
            }
        }

        val accelSpeeds = if (locationSpeeds.isEmpty()) {
            accelWindow.mapNotNull { calculateSpeedFromAccelerometer(it.values) }
        } else {
            emptyList<Double>()
        }

        var averageSpeed = 0.0
        if (locationSpeeds.isNotEmpty()) {
            val avgLocSpeed = locationSpeeds.average()
            if (avgLocSpeed > 0) {
                averageSpeed = avgLocSpeed
            }
        }

        if (averageSpeed == 0.0 && accelSpeeds.isNotEmpty()) {
            averageSpeed = accelSpeeds.average()
        }

        val firstLocationId = speedWindow.firstNotNullOfOrNull { it.locationId }
        val locationData = firstLocationId?.let { locationCache[it] ?: fetchLocationDataById(it) }

        return Pair(averageSpeed, locationData)
    }


    /**
     * Estimates the current speed (in m/s) from the most recent entry in the speedWindow.
     * Prefers location-based speed and falls back to an accelerometer estimate.
     */
    private suspend fun estimateCurrentSpeedMps(window: AnalysisWindow): Double {
        val recent = window.speedWindow.lastOrNull()
        if (recent != null) {
            recent.locationId?.let { locationId ->
                val loc = fetchLocationDataById(locationId)
                if (loc?.speed != null && loc.speed > 0) {
                    Log.d("SpeedEstimation", "Using location-based speed for locationId $locationId: ${loc.speed}")
                    return loc.speed
                }
            }
        }

        val recentAccel = window.accelerometerWindow.lastOrNull()
        if (recentAccel != null) {
            val speedFromAccel = calculateSpeedFromAccelerometer(recentAccel.values)
            if (speedFromAccel != null) {
                Log.d("SpeedEstimation", "Using accelerometer speed: $speedFromAccel")
                return speedFromAccel
            }
        }

        Log.d("SpeedEstimation", "No valid speed data found, returning 0.0")
        return 0.0
    }

    /**
     * Adjusts the threshold based on current speed. For every 10 m/s above 10 m/s,
     * the threshold is reduced by 0.5. This makes the system more sensitive at higher speeds.
     */
    private fun dynamicThresholdAdjustment(speedMps: Double): Float {
        val increments = ((speedMps - 10.0) / 10.0).coerceAtLeast(0.0)
        val reduction = increments * 0.5f
        return reduction.toFloat()
    }

    /**
     * Fetches location data for a given locationId using the repository.
     */
    private suspend fun fetchLocationDataById(locationId: UUID): LocationData? {
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Fetching location data for locationId: $locationId")
        val locationEntity = locationRepository.getLocationById(locationId)
        val locationData = locationEntity?.let {
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
        if (locationData != null) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Fetched location data: $locationData")
        } else {
            Log.w("NewUnsafeDrivingBehaviourAnalyser", "No location data found for locationId: $locationId")
        }
        return locationData
    }

    /**
     * Creates an UnsafeBehaviourModel from the provided sensor data and measured value.
     */
    private fun createUnsafeBehavior(
        behaviorType: String,
        data: RawSensorDataEntity,
        measuredValue: Float,
        context: Context,
        speedLimit: Float? = null
    ): UnsafeBehaviourModel? {
        val tripId = data.tripId
        if (tripId == null) {
            Log.w("NewUnsafeDrivingBehaviourAnalyser", "Skipping unsafe behavior: missing tripId.")
            return null
        }
        val driverProfileId = data.driverProfileId ?: PreferenceUtils.getDriverProfileId(context)
        if (driverProfileId == null) {
            Log.w("NewUnsafeDrivingBehaviourAnalyser", "Skipping unsafe behavior: missing driverProfileId.")
            return null
        }
        val severity = calculateSeverity(behaviorType, measuredValue, speedLimit)
        Log.d(
            "NewUnsafeDrivingBehaviourAnalyser",
            "Creating unsafe behavior. Type=$behaviorType, measuredValue=$measuredValue, severity=$severity"
        )
        return UnsafeBehaviourModel(
            id = UUID.randomUUID(),
            tripId = tripId,
            driverProfileId = driverProfileId,
            behaviorType = behaviorType,
            timestamp = data.timestamp,
            date = data.date ?: Date(),
            updatedAt = null,
            updated = false,
            severity = severity,
            locationId = data.locationId
        )
    }

    /**
     * Calculates a severity value (0 to 1) based on how much the measured value exceeds the threshold.
     */
    private fun calculateSeverity(
        behaviorType: String,
        measuredValue: Float,
        speedLimit: Float? = null
    ): Float {
        val severity = when (behaviorType) {
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
                val effectiveLimit = speedLimit ?: SPEED_LIMIT
                val excess = measuredValue - effectiveLimit
                (excess / MAX_SPEED_EXCESS).coerceIn(0f, 1f)
            }
            else -> 0f
        }
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Calculated severity for $behaviorType: $severity")
        return severity
    }

    // -------------------------------------------------------------------------------
    // Basic magnitude and speed calculations
    // -------------------------------------------------------------------------------
    private fun calculateAccelerationMagnitude(values: List<Float>): Float {
        if (values.size < 3) return 0f
        return sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
    }

    private fun calculateRotationMagnitude(values: List<Float>): Float {
        if (values.size < 3) return 0f
        return sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
    }

    /**
     * Estimates speed from accelerometer data as a fallback when location data is unavailable.
     * This is a simplistic calculation; sensor fusion would yield more accurate speed estimates.
     */
    private fun calculateSpeedFromAccelerometer(values: List<Float>): Double? {
        if (values.size < 3) {
            Log.w("NewUnsafeDrivingBehaviourAnalyser", "Not enough accelerometer values to calculate speed.")
            return null
        }
        val magnitude = calculateAccelerationMagnitude(values)
        val timeInterval = 0.1 // seconds; adjust if your sampling rate differs
        val speed = magnitude * timeInterval
        Log.d(
            "NewUnsafeDrivingBehaviourAnalyser",
            "Calculated speed from accelerometer: $speed (m/s), magnitude=$magnitude, dt=$timeInterval"
        )
        return speed
    }

    companion object {
        const val ACCELEROMETER_TYPE = 1
        const val SPEED_TYPE = 2 // Magnetometer or fallback logic
        const val GYROSCOPE_TYPE = 4
        const val LINEAR_ACCELERATION_TYPE = 10
        const val ROTATION_VECTOR_TYPE = 11

        const val ACCELERATION_THRESHOLD = 4.0f // m/s^2
        const val BRAKING_THRESHOLD = -4.5f // m/s^2
        const val SWERVING_THRESHOLD = 0.14f // rad/s
        const val SPEED_LIMIT = 27.78f // m/s (~100 km/h)
        const val SPEED_TOLERANCE_RATIO = 1.10

        const val SPEEDING_MIN_DURATION_MS = 20_000L
        const val SPEEDING_COOLDOWN_MS = 10_000L
        const val MIN_ACCELERATION_SPEED_MPS = 1.4
        const val MIN_SWERVE_SPEED_MPS = 5.56

        const val ACCELERATION_MIN_DURATION_MS = 300L
        const val EVENT_COOLDOWN_MS = 1_500L

        const val POTHOLE_WINDOW_MS = 500L
        const val POTHOLE_Z_STDDEV_THRESHOLD = 2.0
        const val POTHOLE_Z_PEAK_TO_PEAK = 5.0

        const val ROUGHNESS_WINDOW_MS = 1_000L
        const val ROUGHNESS_MULTIPLIER = 0.35f
        const val MAX_ROUGHNESS_ADJUSTMENT = 3.0f

        const val SWERVE_MAX_DURATION_MS = 1_500L
        const val SWERVE_COOLDOWN_MS = 2_000L

        const val MIN_ACCELERATION_THRESHOLD = 1.5f
        const val MIN_BRAKING_THRESHOLD = 1.5f

        const val MAX_ACCELERATION_EXCESS = 5.5f
        const val MAX_BRAKING_EXCESS = 5.5f
        const val MAX_SWERVING_EXCESS = 0.5f
        const val MAX_SPEED_EXCESS = 2.778f

        // Testing thresholds (very low values for emulator testing).
//        const val ACCELERATION_THRESHOLD = 0.2f // m/s^2
//        const val BRAKING_THRESHOLD = -0.2f // m/s^2
//        const val SWERVING_THRESHOLD = 0.02f // rad/s
//        const val SPEED_LIMIT = 0.02f // 100 km/h in m/s (for demo).
//        const val MAX_ACCELERATION_EXCESS = 0.5f
//        const val MAX_BRAKING_EXCESS = 0.5f
//        const val MAX_SWERVING_EXCESS = 0.1f
//        const val MAX_SPEED_EXCESS = 0.01f // ~10 km/h above 100 km/h.
    }
}


//Old Code
//@Singleton
//class NewUnsafeDrivingBehaviourAnalyser @Inject constructor(
//    private val locationRepository: LocationRepository // Injected repository
//) {
//
//    private val accelerometerWindow = ArrayDeque<RawSensorDataEntity>()
//    private val rotationWindow = ArrayDeque<RawSensorDataEntity>()
//    private val speedWindow = ArrayDeque<RawSensorDataEntity>()
//    private val windowSize = 100  // Adjust based on desired sensitivity and resource constraints
//
//    fun analyze(sensorDataFlow: Flow<RawSensorDataEntity>, context: Context): Flow<UnsafeBehaviourModel> = flow {
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Starting analysis of sensor data flow")
//        sensorDataFlow.collect { data ->
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Received sensor data: sensorType=${data.sensorType} timestamp=${data.timestamp}")
//            when (data.sensorType) {
//                ACCELEROMETER_TYPE -> {
//                    accelerometerWindow.addLast(data)
//                    if (accelerometerWindow.size > windowSize) {
//                        val removed = accelerometerWindow.removeFirst()
//                        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer window exceeded size $windowSize, removed oldest data with timestamp=${removed.timestamp}")
//                    }
//                    val behavior = analyzeAccelerometerData(context)
//                    if (behavior != null) {
//                        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Detected unsafe accelerometer behavior: ${behavior.behaviorType} with severity ${behavior.severity}")
//                        emit(behavior)
//                    }
//                }
//                ROTATION_VECTOR_TYPE -> {
//                    rotationWindow.addLast(data)
//                    if (rotationWindow.size > windowSize) {
//                        val removed = rotationWindow.removeFirst()
//                        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation window exceeded size $windowSize, removed oldest data with timestamp=${removed.timestamp}")
//                    }
//                    val behavior = analyzeRotationData(context)
//                    if (behavior != null) {
//                        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Detected unsafe rotation behavior: ${behavior.behaviorType} with severity ${behavior.severity}")
//                        emit(behavior)
//                    }
//                }
//                SPEED_TYPE -> {
//                    speedWindow.addLast(data)
//                    if (speedWindow.size > windowSize) {
//                        val removed = speedWindow.removeFirst()
//                        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speed window exceeded size $windowSize, removed oldest data with timestamp=${removed.timestamp}")
//                    }
//                    val behavior = analyzeSpeedData(context)
//                    if (behavior != null) {
//                        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Detected unsafe speed behavior: ${behavior.behaviorType} with severity ${behavior.severity}")
//                        emit(behavior)
//                    }
//                }
//                else -> {
//                    Log.w("NewUnsafeDrivingBehaviourAnalyser", "Unknown sensor type: ${data.sensorType}")
//                }
//            }
//        }
//    }
//
//    private fun analyzeAccelerometerData(context: Context): UnsafeBehaviourModel? {
//        if (accelerometerWindow.isEmpty()) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer window is empty, skipping analysis")
//            return null
//        }
//        val rms = sqrt(accelerometerWindow.map { calculateAccelerationMagnitude(it.values).pow(2) }.average())
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer RMS: $rms")
//        return when {
//            rms > ACCELERATION_THRESHOLD -> {
//                Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms exceeds ACCELERATION_THRESHOLD $ACCELERATION_THRESHOLD, detecting Harsh Acceleration")
//                createUnsafeBehavior("Harsh Acceleration", accelerometerWindow.last(), rms.toFloat(), context)
//            }
//            rms < BRAKING_THRESHOLD -> {
//                Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms below BRAKING_THRESHOLD $BRAKING_THRESHOLD, detecting Harsh Braking")
//                createUnsafeBehavior("Harsh Braking", accelerometerWindow.last(), rms.toFloat(), context)
//            }
//            else -> {
//                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer RMS $rms within safe limits")
//                null
//            }
//        }
//    }
//
//    private fun analyzeRotationData(context: Context): UnsafeBehaviourModel? {
//        if (rotationWindow.isEmpty()) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation window is empty, skipping analysis")
//            return null
//        }
//        val rms = sqrt(rotationWindow.map { calculateRotationMagnitude(it.values).pow(2) }.average())
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation RMS: $rms")
//        return if (rms > SWERVING_THRESHOLD) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms exceeds SWERVING_THRESHOLD $SWERVING_THRESHOLD, detecting Swerving")
//            createUnsafeBehavior("Swerving", rotationWindow.last(), rms.toFloat(), context)
//        } else {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation RMS $rms within safe limits")
//            null
//        }
//    }
//
//    private suspend fun analyzeSpeedData(context: Context): UnsafeBehaviourModel? {
//        if (speedWindow.isEmpty()) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speed window is empty, skipping analysis")
//            return null
//        }
//
//        // Collect speeds from location data (if locationId exists)
//        val locationSpeeds = speedWindow.mapNotNull { data ->
//            data.locationId?.let { locationId ->
//                val locationData = fetchLocationDataById(locationId)
//                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Fetched speed ${locationData?.speed} from locationId $locationId")
//                locationData?.speed
//            }
//        }
//
//        // Collect speeds from accelerometer readings if sensorType indicates ACCELEROMETER_TYPE
//        val accelSpeeds = speedWindow.filter { it.sensorType == ACCELEROMETER_TYPE }
//            .mapNotNull { data ->
//                val speedFromAccel = calculateSpeedFromAccelerometer(data.values)
//                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Calculated speed from accelerometer: $speedFromAccel")
//                speedFromAccel
//            }
//
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Location speeds: $locationSpeeds, Accelerometer speeds: $accelSpeeds")
//
//        // Determine the average speed: Prefer location-based speeds if the average is greater than 0.
//        var averageSpeed: Double? = null
//        if (locationSpeeds.isNotEmpty()) {
//            val avgLocationSpeed = locationSpeeds.average()
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Average location-based speed: $avgLocationSpeed")
//            if (avgLocationSpeed > 0) {
//                averageSpeed = avgLocationSpeed
//            }
//        }
//        if ((averageSpeed == null || averageSpeed.toInt()==0) && accelSpeeds.isNotEmpty()) {
//            val avgAccelSpeed = accelSpeeds.average()
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Average accelerometer-based speed: $avgAccelSpeed")
//            averageSpeed = avgAccelSpeed
//        }
//
//        if (averageSpeed == null) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "No valid speed values found, skipping analysis")
//            return null
//        }
//
//        // Fetch a representative location to determine the effective speed limit.
//        val firstLocationId = speedWindow.firstNotNullOfOrNull { it.locationId }
//        val locationData = firstLocationId?.let { fetchLocationDataById(it) }
//        if (locationData == null) {
//            Log.w("NewUnsafeDrivingBehaviourAnalyser", "No location data found for representative location; using default speed limit")
//        } else {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Location data for $firstLocationId: speedLimit=${locationData.speedLimit}")
//        }
//        val effectiveSpeedLimit: Double = if (locationData?.speedLimit != null && locationData.speedLimit.toInt() != 0) {
//            locationData.speedLimit.toDouble()
//        } else {
//            SPEED_LIMIT.toDouble()
//        }
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Effective speed limit determined: $effectiveSpeedLimit")
//
//        // Compare the average speed to the effective speed limit.
//        return if (averageSpeed > effectiveSpeedLimit) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Average speed $averageSpeed exceeds effective speed limit $effectiveSpeedLimit, detecting Speeding")
//            createUnsafeBehavior("Speeding", speedWindow.last(), averageSpeed.toFloat(), context)
//        } else {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Average speed $averageSpeed within safe limits compared to effective speed limit $effectiveSpeedLimit")
//            null
//        }
//    }
//
//    private suspend fun fetchLocationDataById(locationId: UUID): LocationData? {
////        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Fetching location data for locationId: $locationId")
//        val locationEntity = locationRepository.getLocationById(locationId)
//        val locationData = locationEntity?.let {
//            LocationData(
//                id = it.id,
//                latitude = it.latitude,
//                longitude = it.longitude,
//                altitude = it.altitude,
//                speed = it.speed.toDouble(),
//                distance = it.distance.toDouble(),
//                timestamp = it.timestamp,
//                date = it.date,
//                speedLimit = it.speedLimit,
//                processed = it.processed,
//                sync = it.sync
//            )
//        }
//        if (locationData != null) {
//            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Fetched location data: $locationData")
//        } else {
//            Log.w("NewUnsafeDrivingBehaviourAnalyser", "No location data found for locationId: $locationId")
//        }
//        return locationData
//    }
//
//    private fun createUnsafeBehavior(
//        behaviorType: String,
//        data: RawSensorDataEntity,
//        measuredValue: Float,
//        context: Context
//    ): UnsafeBehaviourModel {
//        val severity = calculateSeverity(behaviorType, measuredValue)
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Creating unsafe behavior. Type: $behaviorType, Measured Value: $measuredValue, Severity: $severity")
//        return UnsafeBehaviourModel(
//            id = UUID.randomUUID(),
//            tripId = data.tripId!!,
//            driverProfileId = PreferenceUtils.getDriverProfileId(context)!!,
//            behaviorType = behaviorType,
//            timestamp = data.timestamp,
//            date = data.date!!,
//            updatedAt = null,
//            updated = false,
//            severity = severity,
//            locationId = data.locationId
//        )
//    }
//
//    private fun calculateSeverity(behaviorType: String, measuredValue: Float): Float {
//        val severity = when (behaviorType) {
//            "Harsh Acceleration" -> {
//                val excess = measuredValue - ACCELERATION_THRESHOLD
//                (excess / MAX_ACCELERATION_EXCESS).coerceIn(0f, 1f)
//            }
//            "Harsh Braking" -> {
//                val excess = abs(measuredValue) - abs(BRAKING_THRESHOLD)
//                (excess / MAX_BRAKING_EXCESS).coerceIn(0f, 1f)
//            }
//            "Swerving" -> {
//                val excess = measuredValue - SWERVING_THRESHOLD
//                (excess / MAX_SWERVING_EXCESS).coerceIn(0f, 1f)
//            }
//            "Speeding" -> {
//                val excess = measuredValue - SPEED_LIMIT
//                (excess / MAX_SPEED_EXCESS).coerceIn(0f, 1f)
//            }
//            else -> 0f
//        }
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Calculated severity for $behaviorType: $severity")
//        return severity
//    }
//
//    private fun calculateAccelerationMagnitude(values: List<Float>): Float {
//        val magnitude = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Calculated acceleration magnitude: $magnitude from values: $values")
//        return magnitude
//    }
//
//    private fun calculateRotationMagnitude(values: List<Float>): Float {
//        val magnitude = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Calculated rotation magnitude: $magnitude from values: $values")
//        return magnitude
//    }
//
//    private fun calculateSpeedFromAccelerometer(values: List<Float>): Double? {
//        if (values.size < 3) {
//            Log.w("NewUnsafeDrivingBehaviourAnalyser", "Insufficient accelerometer values to calculate speed from: $values")
//            return null
//        }
//        val magnitude = calculateAccelerationMagnitude(values)
//        val timeInterval = 0.1 // Approximate interval (seconds) between accelerometer readings
//        val speed = magnitude * timeInterval
//        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Calculated speed from accelerometer: $speed using magnitude: $magnitude and timeInterval: $timeInterval")
//        return speed
//    }
//
//    companion object {
//        const val ACCELEROMETER_TYPE = 1
//        const val SPEED_TYPE = 2 // Magnetometer or fallback logic
//        const val ROTATION_VECTOR_TYPE = 11
////
////        const val ACCELERATION_THRESHOLD = 3.5f  // m/s²
////        const val BRAKING_THRESHOLD = -3.5f      // m/s²
////        const val SWERVING_THRESHOLD = 0.1f      // rad/s
////        const val SPEED_LIMIT = 27.78f // m/s (~100 km/h)
////
////        const val MAX_ACCELERATION_EXCESS = 6.5f
////        const val MAX_BRAKING_EXCESS = 6.5f
////        const val MAX_SWERVING_EXCESS = 0.9f
////        const val MAX_SPEED_EXCESS = 2.778f
//
//
//// Testing values
//// Testing Thresholds (very low values for emulator testing).
//const val ACCELERATION_THRESHOLD = 0.2f   // m/s².
//        const val BRAKING_THRESHOLD = -0.2f       // m/s².
//        const val SWERVING_THRESHOLD = 0.02f       // rad/s.
//        const val SPEED_LIMIT = 0.02f            // 100 km/h in m/s (for demo).
//
//        // Maximum excess for severity normalization.
//        const val MAX_ACCELERATION_EXCESS = 0.5f
//        const val MAX_BRAKING_EXCESS = 0.5f
//        const val MAX_SWERVING_EXCESS = 0.1f
//        const val MAX_SPEED_EXCESS = 0.01f       // ~10 km/h above 100 km/h.
//    }
//}
