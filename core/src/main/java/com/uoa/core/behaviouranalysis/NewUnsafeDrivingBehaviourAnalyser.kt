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

    // Sliding windows for each sensor type.
    private val accelerometerWindow = ArrayDeque<RawSensorDataEntity>()
    private val rotationWindow = ArrayDeque<RawSensorDataEntity>()
    private val speedWindow = ArrayDeque<RawSensorDataEntity>()

    // Maximum number of sensor events to hold in each window.
    private val windowSize = 100

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
        sensorDataFlow.collect { data ->
            processSensorData(data, context)?.let { behavior ->
                emit(behavior)
            }
        }
    }

    /**
     * Processes a single sensor data event by updating its window and calling the appropriate analysis.
     */
    private suspend fun processSensorData(
        data: RawSensorDataEntity,
        context: Context
    ): UnsafeBehaviourModel? {
        Log.d(
            "NewUnsafeDrivingBehaviourAnalyser",
            "Processing sensor data: sensorType=${data.sensorType}, timestamp=${data.timestamp}"
        )
        return when (data.sensorType) {
            ACCELEROMETER_TYPE -> {
                accelerometerWindow.addLast(data)
                if (accelerometerWindow.size > windowSize) accelerometerWindow.removeFirst()
                analyzeAccelerometerData(accelerometerWindow, context)
            }
            ROTATION_VECTOR_TYPE -> {
                rotationWindow.addLast(data)
                if (rotationWindow.size > windowSize) rotationWindow.removeFirst()
                analyzeRotationData(rotationWindow, context)
            }
            SPEED_TYPE -> {
                speedWindow.addLast(data)
                if (speedWindow.size > windowSize) speedWindow.removeFirst()
                analyzeSpeedData(speedWindow, context)
            }
            else -> {
                Log.w("NewUnsafeDrivingBehaviourAnalyser", "Unknown sensor type: ${data.sensorType}")
                null
            }
        }
    }

    // -------------------------------------------------------------------------------
    // Accelerometer Analysis
    // -------------------------------------------------------------------------------
    private suspend fun analyzeAccelerometerData(
        window: ArrayDeque<RawSensorDataEntity>,
        context: Context
    ): UnsafeBehaviourModel? {
        if (window.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer window is empty, skipping analysis")
            return null
        }
        val rms = sqrt(window.map { calculateAccelerationMagnitude(it.values).pow(2) }.average())
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer RMS: $rms")
        val currentSpeedMps = estimateCurrentSpeedMps()
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Current speed (m/s): $currentSpeedMps")
        if (currentSpeedMps < 1.4) { // ~5 km/h threshold
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speed under threshold. Skipping harsh accel/braking detection.")
            return null
        }
        val dynamicAccThreshold = ACCELERATION_THRESHOLD + dynamicThresholdAdjustment(currentSpeedMps)
        val dynamicBrakeThreshold = BRAKING_THRESHOLD - dynamicThresholdAdjustment(currentSpeedMps)
        return when {
            rms > dynamicAccThreshold -> {
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms > $dynamicAccThreshold => Harsh Acceleration")
                createUnsafeBehavior("Harsh Acceleration", window.last(), rms.toFloat(), context)
            }
            rms < dynamicBrakeThreshold -> {
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms < $dynamicBrakeThreshold => Harsh Braking")
                createUnsafeBehavior("Harsh Braking", window.last(), rms.toFloat(), context)
            }
            else -> {
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Accelerometer RMS $rms within safe limits")
                null
            }
        }
    }

    // -------------------------------------------------------------------------------
    // Rotation Analysis
    // -------------------------------------------------------------------------------
    private suspend fun analyzeRotationData(
        window: ArrayDeque<RawSensorDataEntity>,
        context: Context
    ): UnsafeBehaviourModel? {
        if (window.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation window is empty, skipping analysis")
            return null
        }
        val rms = sqrt(window.map { calculateRotationMagnitude(it.values).pow(2) }.average())
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation RMS: $rms")
        val currentSpeedMps = estimateCurrentSpeedMps()
        val dynamicSwervingThreshold = if (currentSpeedMps > 5) {
            SWERVING_THRESHOLD - 0.01f
        } else {
            SWERVING_THRESHOLD
        }
        return if (rms > dynamicSwervingThreshold) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "RMS $rms > $dynamicSwervingThreshold => Swerving")
            createUnsafeBehavior("Swerving", window.last(), rms.toFloat(), context)
        } else {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Rotation RMS $rms within safe limits")
            null
        }
    }

    // -------------------------------------------------------------------------------
    // Speed Analysis
    // -------------------------------------------------------------------------------
    private suspend fun analyzeSpeedData(
        window: ArrayDeque<RawSensorDataEntity>,
        context: Context
    ): UnsafeBehaviourModel? {
        if (window.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speed window is empty, skipping analysis")
            return null
        }
        val (avgSpeed, locationData) = computeAverageSpeedAndLocation(window)
        if (avgSpeed == 0.0) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Computed average speed is 0.0; skipping analysis")
            return null
        }
        val effectiveSpeedLimit: Double = locationData?.speedLimit?.takeIf { it.toInt() != 0 }
            ?.toDouble() ?: SPEED_LIMIT.toDouble()
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Effective speed limit: $effectiveSpeedLimit, avgSpeed=$avgSpeed")
        return if (avgSpeed > effectiveSpeedLimit) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Speeding detected")
            createUnsafeBehavior("Speeding", window.last(), avgSpeed.toFloat(), context)
        } else {
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
        speedWindow: ArrayDeque<RawSensorDataEntity>
    ): Pair<Double, LocationData?> {
        // Log the size and content of the speedWindow
        Log.d("SpeedCalculation", "Speed window size: ${speedWindow.size}, contents: $speedWindow")

        // Log the location speeds before processing
        val locationSpeeds = speedWindow.mapNotNull { data ->
            data.locationId?.let { locationId ->
                val locationData = fetchLocationDataById(locationId)
                locationData?.speed?.also {
                    Log.d("SpeedCalculation", "Found speed for locationId $locationId: $it")
                }
            }
        }

        // Log if locationSpeeds is empty and accelerometer speeds are being considered
        val accelSpeeds = if (locationSpeeds.isEmpty()) {
            accelerometerWindow.mapNotNull { calculateSpeedFromAccelerometer(it.values) }
                .also {
                    Log.d("SpeedCalculation", "Location speeds empty, falling back to accelerometer speeds: $it")
                }
        } else {
            emptyList<Double>()
        }

        // Calculate the average speed
        var averageSpeed = 0.0
        if (locationSpeeds.isNotEmpty()) {
            val avgLocSpeed = locationSpeeds.average()
            if (avgLocSpeed > 0) {
                averageSpeed = avgLocSpeed
                Log.d("SpeedCalculation", "Average location speed: $avgLocSpeed")
            }
        }

        if (averageSpeed == 0.0 && accelSpeeds.isNotEmpty()) {
            averageSpeed = accelSpeeds.average()
            Log.d("SpeedCalculation", "Average accelerometer speed: ${accelSpeeds.average()}")
        }

        // Log the first location ID used
        val firstLocationId = speedWindow.firstNotNullOfOrNull { it.locationId }
        Log.d("SpeedCalculation", "First location ID from window: $firstLocationId")

        // Fetch the location data for the first location ID
        val locationData = firstLocationId?.let { fetchLocationDataById(it) }
        Log.d("SpeedCalculation", "Location data for locationId $firstLocationId: $locationData")

        return Pair(averageSpeed, locationData)
    }


    /**
     * Estimates the current speed (in m/s) from the most recent entry in the speedWindow.
     * Prefers location-based speed and falls back to an accelerometer estimate.
     */
    private suspend fun estimateCurrentSpeedMps(): Double {
        // Log the contents of the speedWindow
        val recent = speedWindow.lastOrNull()
        Log.d("SpeedEstimation", "Recent entry in speed window: $recent")

        if (recent == null) {
            Log.d("SpeedEstimation", "No recent data available, returning 0.0")
            return 0.0
        }

        // Attempt to get speed from location data
        recent.locationId?.let { locationId ->
            val loc = fetchLocationDataById(locationId)
            if (loc != null) {
                Log.d("SpeedEstimation", "Using location-based speed for locationId $locationId: ${loc.speed}")
                return loc.speed!!
            } else {
                Log.d("SpeedEstimation", "Location data for locationId $locationId is null")
            }
        }

        // Fallback to accelerometer speed calculation
        if (recent.sensorType == ACCELEROMETER_TYPE) {
            val speedFromAccel = calculateSpeedFromAccelerometer(recent.values)
            if (speedFromAccel != null) {
                Log.d("SpeedEstimation", "Using accelerometer speed: $speedFromAccel")
                return speedFromAccel
            } else {
                Log.d("SpeedEstimation", "Accelerometer speed calculation failed")
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
        context: Context
    ): UnsafeBehaviourModel {
        val severity = calculateSeverity(behaviorType, measuredValue)
        Log.d(
            "NewUnsafeDrivingBehaviourAnalyser",
            "Creating unsafe behavior. Type=$behaviorType, measuredValue=$measuredValue, severity=$severity"
        )
        return UnsafeBehaviourModel(
            id = UUID.randomUUID(),
            tripId = data.tripId ?: UUID.randomUUID(),
            driverProfileId = PreferenceUtils.getDriverProfileId(context) ?: UUID.randomUUID(),
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
    private fun calculateSeverity(behaviorType: String, measuredValue: Float): Float {
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
                val excess = measuredValue - SPEED_LIMIT
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
        const val ROTATION_VECTOR_TYPE = 11
//
        const val ACCELERATION_THRESHOLD = 3.5f  // m/s²
        const val BRAKING_THRESHOLD = -3.5f      // m/s²
        const val SWERVING_THRESHOLD = 0.2f      // rad/s
        const val SPEED_LIMIT = 27.78f // m/s (~100 km/h)

        const val MAX_ACCELERATION_EXCESS = 5.5f
        const val MAX_BRAKING_EXCESS = 5.5f
        const val MAX_SWERVING_EXCESS = 0.5f
        const val MAX_SPEED_EXCESS = 2.778f
//
//
//// Testing values
//// Testing Thresholds (very low values for emulator testing).
//        const val ACCELERATION_THRESHOLD = 0.2f   // m/s².
//        const val BRAKING_THRESHOLD = -0.2f       // m/s².
//        const val SWERVING_THRESHOLD = 0.02f       // rad/s.
//        const val SPEED_LIMIT = 0.02f            // 100 km/h in m/s (for demo).
//
//        // Maximum excess for severity normalization.
//        const val MAX_ACCELERATION_EXCESS = 0.5f
//        const val MAX_BRAKING_EXCESS = 0.5f
//        const val MAX_SWERVING_EXCESS = 0.1f
//        const val MAX_SPEED_EXCESS = 0.01f       // ~10 km/h above 100 km/h.
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