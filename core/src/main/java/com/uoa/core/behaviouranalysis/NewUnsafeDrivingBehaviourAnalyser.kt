package com.uoa.core.behaviouranalysis

import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.PreferenceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
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
        val longRotationWindow: ArrayDeque<RawSensorDataEntity> = ArrayDeque(),
        var hasLinearAcceleration: Boolean = false,
        var lastSpeedAnalysisTimeMs: Long = 0L,
        var accelCandidateStartMs: Long = 0L,
        var brakeCandidateStartMs: Long = 0L,
        var lastAccelerationEventMs: Long = 0L,
        var lastBrakingEventMs: Long = 0L,
        var corneringCandidateStartMs: Long = 0L,
        var lastCorneringEventMs: Long = 0L,
        var lastAggressiveTurnEventMs: Long = 0L,
        var lastAggressiveStopGoEventMs: Long = 0L,
        var speedingStartMs: Long = 0L,
        var lastSpeedingEventMs: Long = 0L,
        var lastSwervingEventMs: Long = 0L,
        var lastPhoneHandlingEventMs: Long = 0L,
        var lastFatigueEventMs: Long = 0L,
        var lastRoughRoadEventMs: Long = 0L,
        var lastCrashEventMs: Long = 0L,
        val locationCache: MutableMap<UUID, LocationData> = mutableMapOf(),
        val lastLocationByTrip: MutableMap<UUID, LocationData> = mutableMapOf(),
        val lastHeadingByTrip: MutableMap<UUID, Double> = mutableMapOf(),
        val lastRotationMatrixByTrip: MutableMap<UUID, FloatArray> = mutableMapOf(),
        val lastRotationVectorTimestampByTrip: MutableMap<UUID, Long> = mutableMapOf(),
        val lastRotationMissingLogMsByTrip: MutableMap<UUID, Long> = mutableMapOf(),
        val lastHeadingMissingLogMsByTrip: MutableMap<UUID, Long> = mutableMapOf(),
        val crashCandidatesByTrip: MutableMap<UUID, CrashCandidate> = mutableMapOf(),
        val lastForwardAccelByTrip: MutableMap<UUID, Pair<Double, Long>> = mutableMapOf(),
        val tripStartTimestampByTrip: MutableMap<UUID, Long> = mutableMapOf()
    )

    // Maximum number of sensor events to hold in each window.
    private val windowSize = 200
    private val longRotationWindowSize = 3_000
    private val speedAnalysisIntervalMs = 1_000L
    private val headingSampleMinDistanceM = 5.0

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
        data.tripId?.let { tripId ->
            if (!window.tripStartTimestampByTrip.containsKey(tripId)) {
                window.tripStartTimestampByTrip[tripId] = data.timestamp
            }
        }
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
                addToWindow(window.longRotationWindow, data, longRotationWindowSize)
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

    private fun addToWindow(
        window: ArrayDeque<RawSensorDataEntity>,
        data: RawSensorDataEntity,
        maxSize: Int = windowSize
    ) {
        window.addLast(data)
        if (window.size > maxSize) {
            window.removeFirst()
        }
    }

    private fun shouldLogRateLimited(
        map: MutableMap<UUID, Long>,
        tripId: UUID,
        now: Long,
        intervalMs: Long
    ): Boolean {
        val last = map[tripId] ?: 0L
        return if (now - last >= intervalMs) {
            map[tripId] = now
            true
        } else {
            false
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

    private suspend fun estimateSignedAcceleration(
        window: AnalysisWindow,
        accelWindow: ArrayDeque<RawSensorDataEntity>
    ): Double? {
        if (accelWindow.isEmpty()) return null
        val sampleCount = minOf(5, accelWindow.size)
        val samples = accelWindow.toList().takeLast(sampleCount)
        var forwardSum = 0.0
        var count = 0
        for (sample in samples) {
            val heading = resolveHeadingDegrees(window, sample) ?: continue
            val earth = resolveEarthFrameValues(window, sample) ?: continue
            val vehicle = toVehicleFrame(earth, heading) ?: continue
            forwardSum += vehicle.first
            count++
        }
        if (count == 0) return null
        return forwardSum / count
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

    private fun computeVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }

    private fun computeGyroMagnitudeVariance(samples: List<RawSensorDataEntity>): Double {
        val magnitudes = samples.map { calculateRotationMagnitude(it.values).toDouble() }
        return computeVariance(magnitudes)
    }

    private fun isCircadianDip(timestampMs: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMs
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 2..6 || hour in 14..16
    }

    private fun resolveAccelerationVector(
        window: AnalysisWindow,
        sample: RawSensorDataEntity
    ): Triple<Double, Double, Double>? {
        if (sample.values.size < 3) return null
        return resolveEarthFrameValues(window, sample) ?: Triple(
            sample.values[0].toDouble(),
            sample.values[1].toDouble(),
            sample.values[2].toDouble()
        )
    }

    private fun calculateVectorMagnitude(vector: Triple<Double, Double, Double>): Double {
        return sqrt(vector.first * vector.first + vector.second * vector.second + vector.third * vector.third)
    }

    private fun integrateDeltaV(
        window: AnalysisWindow,
        samples: List<RawSensorDataEntity>
    ): Double? {
        if (samples.size < 2) return null
        val sortedSamples = samples.sortedBy { it.timestamp }
        var deltaV = 0.0
        var prev = sortedSamples.first()
        for (index in 1 until sortedSamples.size) {
            val current = sortedSamples[index]
            val dt = (current.timestamp - prev.timestamp) / 1000.0
            if (dt <= 0.0) {
                prev = current
                continue
            }
            val vector = resolveAccelerationVector(window, current)
            if (vector == null) {
                prev = current
                continue
            }
            deltaV += calculateVectorMagnitude(vector) * dt
            prev = current
        }
        return deltaV
    }

    private fun detectFreeFall(samples: List<RawSensorDataEntity>): Boolean {
        if (samples.size < 2) return false
        var durationMs = 0L
        var lastTimestamp = samples.first().timestamp
        for (sample in samples) {
            val magnitude = calculateAccelerationMagnitude(sample.values)
            if (magnitude <= CRASH_FREEFALL_THRESHOLD_MPS2) {
                durationMs += (sample.timestamp - lastTimestamp).coerceAtLeast(0L)
            } else {
                durationMs = 0L
            }
            lastTimestamp = sample.timestamp
            if (durationMs >= CRASH_FREEFALL_MIN_DURATION_MS) {
                return true
            }
        }
        return false
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

    private suspend fun computeMaxYawRate(
        window: AnalysisWindow,
        samples: List<RawSensorDataEntity>
    ): Double {
        var maxRate = 0.0
        for (sample in samples) {
            val earth = resolveEarthFrameValues(window, sample) ?: continue
            val yaw = abs(earth.third)
            if (yaw > maxRate) {
                maxRate = yaw
            }
        }
        return maxRate
    }

    private fun lowPassYawSeries(
        yawSeries: List<Pair<Long, Double>>,
        cutoffHz: Double
    ): List<Pair<Long, Double>> {
        if (yawSeries.isEmpty()) return yawSeries
        val rc = 1.0 / (2.0 * Math.PI * cutoffHz)
        val output = ArrayList<Pair<Long, Double>>(yawSeries.size)
        var lastValue = yawSeries.first().second
        var lastTimestamp = yawSeries.first().first
        output.add(yawSeries.first())
        for (index in 1 until yawSeries.size) {
            val (timestamp, value) = yawSeries[index]
            val dt = ((timestamp - lastTimestamp).coerceAtLeast(1L) / 1000.0)
            val alpha = (dt / (rc + dt)).coerceIn(0.0, 1.0)
            lastValue = lastValue + alpha * (value - lastValue)
            output.add(timestamp to lastValue)
            lastTimestamp = timestamp
        }
        return output
    }

    private fun computeYawReversalRate(yawSeries: List<Pair<Long, Double>>): Double {
        if (yawSeries.size < 2) return 0.0
        var reversals = 0
        var lastSign = 0
        for ((_, yaw) in yawSeries) {
            val sign = when {
                yaw > SWERVE_SIGN_EPS -> 1
                yaw < -SWERVE_SIGN_EPS -> -1
                else -> 0
            }
            if (sign != 0 && lastSign != 0 && sign != lastSign) {
                reversals++
            }
            if (sign != 0) {
                lastSign = sign
            }
        }
        val durationSeconds = (yawSeries.last().first - yawSeries.first().first) / 1000.0
        if (durationSeconds <= 0.0) return 0.0
        return reversals / durationSeconds
    }

    private fun computeMaxAbsYaw(yawSeries: List<Pair<Long, Double>>): Double {
        var maxAbs = 0.0
        for ((_, yaw) in yawSeries) {
            val absYaw = kotlin.math.abs(yaw)
            if (absYaw > maxAbs) {
                maxAbs = absYaw
            }
        }
        return maxAbs
    }

    private fun computeRqi(
        window: AnalysisWindow,
        samples: List<RawSensorDataEntity>
    ): Double? {
        val vertical = samples.mapNotNull { resolveEarthFrameValues(window, it)?.third }
        if (vertical.isEmpty()) return null
        val meanSquare = vertical.map { it * it }.average()
        return kotlin.math.sqrt(meanSquare)
    }

    private suspend fun buildYawSeries(
        window: AnalysisWindow,
        samples: List<RawSensorDataEntity>
    ): List<Pair<Long, Double>> {
        val series = mutableListOf<Pair<Long, Double>>()
        for (sample in samples) {
            val earth = resolveEarthFrameValues(window, sample) ?: continue
            series.add(sample.timestamp to earth.third)
        }
        return series
    }

    private fun hasSwerveSignature(yawSeries: List<Pair<Long, Double>>): Boolean {
        if (yawSeries.size < 3) return false
        var maxPositive = 0.0
        var maxNegative = 0.0
        var lastSign = 0
        var signChanges = 0

        for ((_, yaw) in yawSeries) {
            if (yaw > maxPositive) {
                maxPositive = yaw
            }
            if (yaw < maxNegative) {
                maxNegative = yaw
            }
            val sign = when {
                yaw > SWERVE_SIGN_EPS -> 1
                yaw < -SWERVE_SIGN_EPS -> -1
                else -> 0
            }
            if (sign != 0 && lastSign != 0 && sign != lastSign) {
                signChanges++
            }
            if (sign != 0) {
                lastSign = sign
            }
        }

        return maxPositive >= SWERVING_THRESHOLD &&
            kotlin.math.abs(maxNegative) >= SWERVING_THRESHOLD &&
            signChanges >= 1
    }

    private data class VehicleAccelSummary(
        val forwardMean: Double,
        val lateralMeanAbs: Double,
        val sampleCount: Int
    )

    private data class CrashCandidate(
        val timestampMs: Long,
        val deltaVMps: Double,
        val sample: RawSensorDataEntity
    )

    private enum class GpsQuality {
        PRECISE,
        APPROXIMATE,
        POOR,
        UNKNOWN
    }

    private fun resolveRotationMatrix(
        window: AnalysisWindow,
        sample: RawSensorDataEntity
    ): FloatArray? {
        val tripId = sample.tripId ?: return null
        val rotationSample = window.rotationWindow.toList()
            .asReversed()
            .firstOrNull {
                it.sensorType == ROTATION_VECTOR_TYPE &&
                    it.values.size >= 3 &&
                    kotlin.math.abs(sample.timestamp - it.timestamp) <= ROTATION_VECTOR_MAX_AGE_MS
            }
        if (rotationSample != null) {
            val matrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(matrix, rotationSample.values.toFloatArray())
            window.lastRotationMatrixByTrip[tripId] = matrix
            window.lastRotationVectorTimestampByTrip[tripId] = rotationSample.timestamp
            Log.d(
                "NewUnsafeDrivingBehaviourAnalyser",
                "Rotation matrix refreshed tripId=$tripId ageMs=${kotlin.math.abs(sample.timestamp - rotationSample.timestamp)}"
            )
            return matrix
        }
        val cached = window.lastRotationMatrixByTrip[tripId]
        if (cached != null && shouldLogRateLimited(
                window.lastRotationMissingLogMsByTrip,
                tripId,
                sample.timestamp,
                LOG_RATE_LIMIT_MS
            )
        ) {
            val lastTs = window.lastRotationVectorTimestampByTrip[tripId]
            Log.d(
                "NewUnsafeDrivingBehaviourAnalyser",
                "Using cached rotation matrix tripId=$tripId lastAgeMs=${lastTs?.let { sample.timestamp - it }}"
            )
        }
        return cached
    }

    private fun transformDeviceToWorld(
        values: List<Float>,
        rotationMatrix: FloatArray
    ): Triple<Double, Double, Double>? {
        if (values.size < 3 || rotationMatrix.size < 9) return null
        val x = rotationMatrix[0] * values[0] + rotationMatrix[1] * values[1] + rotationMatrix[2] * values[2]
        val y = rotationMatrix[3] * values[0] + rotationMatrix[4] * values[1] + rotationMatrix[5] * values[2]
        val z = rotationMatrix[6] * values[0] + rotationMatrix[7] * values[1] + rotationMatrix[8] * values[2]
        return Triple(x.toDouble(), y.toDouble(), z.toDouble())
    }

    private fun resolveEarthFrameValues(
        window: AnalysisWindow,
        sample: RawSensorDataEntity
    ): Triple<Double, Double, Double>? {
        if (sample.values.size < 3) return null
        return when (sample.sensorType) {
            LINEAR_ACCELERATION_TYPE,
            GYROSCOPE_TYPE -> Triple(
                sample.values[0].toDouble(),
                sample.values[1].toDouble(),
                sample.values[2].toDouble()
            )
            ACCELEROMETER_TYPE -> {
                val rotationMatrix = resolveRotationMatrix(window, sample) ?: run {
                    val tripId = sample.tripId
                    if (tripId != null && shouldLogRateLimited(
                            window.lastRotationMissingLogMsByTrip,
                            tripId,
                            sample.timestamp,
                            LOG_RATE_LIMIT_MS
                        )
                    ) {
                        Log.d(
                            "NewUnsafeDrivingBehaviourAnalyser",
                            "Rotation matrix unavailable tripId=$tripId; cannot align accelerometer"
                        )
                    }
                    return null
                }
                val earth = transformDeviceToWorld(sample.values, rotationMatrix) ?: return null
                Triple(earth.first, earth.second, earth.third - GRAVITY_MPS2)
            }
            else -> null
        }
    }

    private suspend fun resolveLocationData(
        window: AnalysisWindow,
        locationId: UUID
    ): LocationData? {
        return window.locationCache[locationId] ?: fetchLocationDataById(locationId)?.also {
            window.locationCache[locationId] = it
        }
    }

    private suspend fun resolveHeadingDegrees(
        window: AnalysisWindow,
        sample: RawSensorDataEntity
    ): Double? {
        val tripId = sample.tripId ?: return null
        val locationId = sample.locationId
        if (locationId == null) {
            val cached = window.lastHeadingByTrip[tripId]
            if (cached == null && shouldLogRateLimited(
                    window.lastHeadingMissingLogMsByTrip,
                    tripId,
                    sample.timestamp,
                    LOG_RATE_LIMIT_MS
                )
            ) {
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Heading unavailable tripId=$tripId (no locationId)")
            }
            return cached
        }
        val location = resolveLocationData(window, locationId)
        if (location == null) {
            val cached = window.lastHeadingByTrip[tripId]
            if (cached == null && shouldLogRateLimited(
                    window.lastHeadingMissingLogMsByTrip,
                    tripId,
                    sample.timestamp,
                    LOG_RATE_LIMIT_MS
                )
            ) {
                Log.d("NewUnsafeDrivingBehaviourAnalyser", "Heading unavailable tripId=$tripId (no location data)")
            }
            return cached
        }

        val cachedHeading = window.lastHeadingByTrip[tripId]
        val accuracy = location.accuracy
        if (accuracy != null && accuracy > MAX_HEADING_ACCURACY_M) {
            if (shouldLogRateLimited(
                    window.lastHeadingMissingLogMsByTrip,
                    tripId,
                    sample.timestamp,
                    LOG_RATE_LIMIT_MS
                )
            ) {
                Log.d(
                    "NewUnsafeDrivingBehaviourAnalyser",
                    "Heading suppressed tripId=$tripId accuracy=$accuracy"
                )
            }
            return cachedHeading
        }
        val speed = location.speed
        if (speed != null && speed < MIN_HEADING_SPEED_MPS) {
            if (shouldLogRateLimited(
                    window.lastHeadingMissingLogMsByTrip,
                    tripId,
                    sample.timestamp,
                    LOG_RATE_LIMIT_MS
                )
            ) {
                Log.d(
                    "NewUnsafeDrivingBehaviourAnalyser",
                    "Heading suppressed tripId=$tripId speed=$speed"
                )
            }
            return cachedHeading
        }

        val previous = window.lastLocationByTrip[tripId]
        window.lastLocationByTrip[tripId] = location

        if (previous == null) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Heading pending tripId=$tripId (awaiting next fix)")
            return cachedHeading
        }

        val distance = location.distance ?: 0.0
        if (distance < headingSampleMinDistanceM) {
            return cachedHeading
        }

        val heading = calculateBearing(previous.latitude, previous.longitude, location.latitude, location.longitude)
        if (heading.isFinite()) {
            window.lastHeadingByTrip[tripId] = heading
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Heading updated tripId=$tripId heading=$heading")
        }
        return window.lastHeadingByTrip[tripId]
    }

    private fun toVehicleFrame(
        earthValues: Triple<Double, Double, Double>,
        headingDegrees: Double
    ): Triple<Double, Double, Double>? {
        val headingRad = Math.toRadians(headingDegrees)
        val xEast = earthValues.first
        val yNorth = earthValues.second
        val zUp = earthValues.third

        val forward = yNorth * kotlin.math.cos(headingRad) + xEast * kotlin.math.sin(headingRad)
        val lateral = -yNorth * kotlin.math.sin(headingRad) + xEast * kotlin.math.cos(headingRad)
        return Triple(forward, lateral, zUp)
    }

    private suspend fun computeVehicleAccelSummary(
        window: AnalysisWindow,
        samples: List<RawSensorDataEntity>
    ): VehicleAccelSummary? {
        var forwardSum = 0.0
        var lateralSum = 0.0
        var count = 0

        for (sample in samples) {
            val heading = resolveHeadingDegrees(window, sample) ?: continue
            val earth = resolveEarthFrameValues(window, sample) ?: continue
            val vehicle = toVehicleFrame(earth, heading) ?: continue
            forwardSum += vehicle.first
            lateralSum += abs(vehicle.second)
            count++
        }

        if (count == 0) return null
        return VehicleAccelSummary(
            forwardMean = forwardSum / count,
            lateralMeanAbs = lateralSum / count,
            sampleCount = count
        )
    }

    private suspend fun resolveForwardAcceleration(
        window: AnalysisWindow,
        sample: RawSensorDataEntity
    ): Double? {
        val heading = resolveHeadingDegrees(window, sample) ?: return null
        val earth = resolveEarthFrameValues(window, sample) ?: return null
        val vehicle = toVehicleFrame(earth, heading) ?: return null
        return vehicle.first
    }

    private suspend fun computeHeadingDeltaDegrees(
        window: AnalysisWindow,
        samples: List<RawSensorDataEntity>
    ): Double? {
        var firstHeading: Double? = null
        var lastHeading: Double? = null
        for (sample in samples) {
            val heading = resolveHeadingDegrees(window, sample) ?: continue
            if (firstHeading == null) {
                firstHeading = heading
            }
            lastHeading = heading
        }
        if (firstHeading == null || lastHeading == null) return null
        return angularDifferenceDegrees(firstHeading, lastHeading)
    }

    private fun angularDifferenceDegrees(start: Double, end: Double): Double {
        var diff = kotlin.math.abs(end - start) % 360.0
        if (diff > 180.0) {
            diff = 360.0 - diff
        }
        return diff
    }

    private fun resolveGpsQuality(locationData: LocationData): GpsQuality {
        val accuracy = locationData.accuracy?.takeIf { it > 0.0 }
        return when {
            accuracy == null -> GpsQuality.UNKNOWN
            accuracy <= GPS_ACCURACY_PRECISE_M -> GpsQuality.PRECISE
            accuracy <= GPS_ACCURACY_APPROX_M -> GpsQuality.APPROXIMATE
            else -> GpsQuality.POOR
        }
    }

    private fun qualityWeight(quality: GpsQuality): Double {
        // Favor precise fixes but keep approximate/unknown readings in the mix.
        return when (quality) {
            GpsQuality.PRECISE -> 1.0
            GpsQuality.APPROXIMATE -> 0.7
            GpsQuality.POOR -> 0.4
            GpsQuality.UNKNOWN -> 0.6
        }
    }

    private fun inferContextualSpeedLimit(
        avgSpeed: Double,
        locationData: LocationData?
    ): Double {
        val speed = locationData?.speed ?: avgSpeed
        return if (speed >= HIGHWAY_SPEED_THRESHOLD_MPS) {
            DEFAULT_HIGHWAY_SPEED_LIMIT_MPS
        } else {
            DEFAULT_URBAN_SPEED_LIMIT_MPS
        }
    }

    private fun calculateBearing(
        prevLat: Double,
        prevLon: Double,
        currLat: Double,
        currLon: Double
    ): Double {
        val lat1 = Math.toRadians(prevLat)
        val lon1 = Math.toRadians(prevLon)
        val lat2 = Math.toRadians(currLat)
        val lon2 = Math.toRadians(currLon)

        val dLon = lon2 - lon1
        val x = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
        val y = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
            kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)

        var bearing = kotlin.math.atan2(x, y)
        bearing = Math.toDegrees(bearing)
        bearing = (bearing + 360) % 360
        return bearing
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
        val eventSample = accelWindow.last()
        val now = eventSample.timestamp
        val currentSpeedMps = estimateCurrentSpeedMps(window)
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Current speed (m/s): $currentSpeedMps")
        val tripId = eventSample.tripId
        if (tripId != null) {
            val existingCandidate = window.crashCandidatesByTrip[tripId]
            if (existingCandidate != null && now - existingCandidate.timestampMs >= CRASH_CANDIDATE_TTL_MS) {
                window.crashCandidatesByTrip.remove(tripId)
            }
        }
        if (tripId != null &&
            now - window.lastCrashEventMs >= CRASH_COOLDOWN_MS &&
            !window.crashCandidatesByTrip.containsKey(tripId)
        ) {
            val magnitude = calculateAccelerationMagnitude(eventSample.values).toDouble()
            if (magnitude >= CRASH_ACCEL_THRESHOLD_MPS2) {
                val crashSamples = recentSamples(accelWindow, CRASH_INTEGRATION_WINDOW_MS)
                val deltaV = integrateDeltaV(window, crashSamples)
                val freeFallSamples = recentSamples(accelWindow, CRASH_FREEFALL_WINDOW_MS)
                val isFreeFall = !window.hasLinearAcceleration && detectFreeFall(freeFallSamples)
                if (!isFreeFall && deltaV != null && deltaV >= CRASH_DELTA_V_THRESHOLD_MPS) {
                    window.crashCandidatesByTrip[tripId] = CrashCandidate(
                        timestampMs = now,
                        deltaVMps = deltaV,
                        sample = eventSample
                    )
                    Log.d(
                        "NewUnsafeDrivingBehaviourAnalyser",
                        "Crash candidate tripId=$tripId magnitude=$magnitude deltaV=$deltaV"
                    )
                } else {
                    Log.d(
                        "NewUnsafeDrivingBehaviourAnalyser",
                        "Crash candidate rejected freeFall=$isFreeFall deltaV=$deltaV magnitude=$magnitude"
                    )
                }
            }
        }

        val forwardAcceleration = if (tripId != null) {
            resolveForwardAcceleration(window, eventSample)
        } else {
            null
        }
        val lastForward = if (tripId != null) window.lastForwardAccelByTrip[tripId] else null
        if (forwardAcceleration != null && tripId != null) {
            window.lastForwardAccelByTrip[tripId] = forwardAcceleration to now
        }
        if (forwardAcceleration != null && lastForward != null) {
            val dt = (now - lastForward.second) / 1000.0
            if (dt > 0.0) {
                val jerk = (forwardAcceleration - lastForward.first) / dt
                if (kotlin.math.abs(jerk) >= AGGRESSIVE_STOP_GO_JERK_THRESHOLD &&
                    currentSpeedMps >= MIN_AGGRESSIVE_STOP_GO_SPEED_MPS &&
                    now - window.lastAggressiveStopGoEventMs >= AGGRESSIVE_STOP_GO_COOLDOWN_MS
                ) {
                    window.lastAggressiveStopGoEventMs = now
                    Log.d(
                        "NewUnsafeDrivingBehaviourAnalyser",
                        "Aggressive stop-and-go detected jerk=$jerk speed=$currentSpeedMps"
                    )
                    return createUnsafeBehavior(
                        "Aggressive Stop-and-Go",
                        eventSample,
                        kotlin.math.abs(jerk).toFloat(),
                        context
                    )
                }
            }
        }

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
        val recentAccelSamples = recentSamples(accelWindow, POTHOLE_WINDOW_MS).ifEmpty {
            accelWindow.toList().takeLast(5)
        }
        val vehicleSummary = computeVehicleAccelSummary(window, recentAccelSamples)
        val signedAccel = vehicleSummary?.forwardMean ?: estimateSignedAcceleration(window, accelWindow)
        if (signedAccel == null) {
            Log.d(
                "NewUnsafeDrivingBehaviourAnalyser",
                "Unable to resolve vehicle-frame acceleration; missing heading or rotation alignment."
            )
            window.accelCandidateStartMs = 0L
            window.brakeCandidateStartMs = 0L
            return null
        }
        val supportsBraking = window.hasLinearAcceleration
        var behavior: UnsafeBehaviourModel? = when {
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
            else -> null
        }

        if (behavior != null) {
            return behavior
        }

        behavior = if (
            vehicleSummary != null &&
            vehicleSummary.lateralMeanAbs > CORNERING_THRESHOLD &&
            currentSpeedMps >= MIN_CORNERING_SPEED_MPS
        ) {
            if (window.corneringCandidateStartMs == 0L) {
                window.corneringCandidateStartMs = now
            }
            val elapsed = now - window.corneringCandidateStartMs
            if (elapsed >= CORNERING_MIN_DURATION_MS &&
                now - window.lastCorneringEventMs >= CORNERING_COOLDOWN_MS
            ) {
                window.corneringCandidateStartMs = 0L
                window.lastCorneringEventMs = now
                Log.d(
                    "NewUnsafeDrivingBehaviourAnalyser",
                    "Lateral accel ${vehicleSummary.lateralMeanAbs} > $CORNERING_THRESHOLD => Harsh Cornering"
                )
                createUnsafeBehavior(
                    "Harsh Cornering",
                    accelWindow.last(),
                    vehicleSummary.lateralMeanAbs.toFloat(),
                    context
                )
            } else {
                null
            }
        } else {
            window.corneringCandidateStartMs = 0L
            null
        }

        if (behavior == null) {
            window.accelCandidateStartMs = 0L
            window.brakeCandidateStartMs = 0L
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Acceleration within safe limits")
        }

        if (behavior != null) {
            return behavior
        }

        val turnSamples = recentSamples(accelWindow, AGGRESSIVE_TURN_WINDOW_MS)
        val headingDelta = computeHeadingDeltaDegrees(window, turnSamples)
        val isAggressiveTurn = headingDelta != null &&
            headingDelta >= AGGRESSIVE_TURN_HEADING_DEG &&
            vehicleSummary != null &&
            vehicleSummary.lateralMeanAbs >= AGGRESSIVE_TURN_LATERAL_THRESHOLD &&
            currentSpeedMps >= MIN_AGGRESSIVE_TURN_SPEED_MPS &&
            now - window.lastAggressiveTurnEventMs >= AGGRESSIVE_TURN_COOLDOWN_MS

        return if (isAggressiveTurn) {
            window.lastAggressiveTurnEventMs = now
            Log.d(
                "NewUnsafeDrivingBehaviourAnalyser",
                "Aggressive turn detected headingDelta=$headingDelta lateral=${vehicleSummary?.lateralMeanAbs} speed=$currentSpeedMps"
            )
            createUnsafeBehavior(
                "Aggressive Turn",
                accelWindow.last(),
                vehicleSummary!!.lateralMeanAbs.toFloat(),
                context
            )
        } else {
            null
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
        val now = rotationWindow.last().timestamp
        val gyroSamples = rotationWindow.filter { it.sensorType == GYROSCOPE_TYPE }
        if (gyroSamples.isEmpty()) {
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "No gyroscope samples; skipping rotation analysis.")
            return null
        }

        if (currentSpeedMps >= PHONE_HANDLING_MIN_SPEED_MPS &&
            now - window.lastPhoneHandlingEventMs >= PHONE_HANDLING_COOLDOWN_MS
        ) {
            val handlingSamples = recentSamples(gyroSamples, PHONE_HANDLING_WINDOW_MS)
            if (handlingSamples.size >= PHONE_HANDLING_MIN_SAMPLES) {
                val variance = computeGyroMagnitudeVariance(handlingSamples)
                Log.d(
                    "NewUnsafeDrivingBehaviourAnalyser",
                    "Phone handling variance=$variance speed=$currentSpeedMps"
                )
                if (variance >= PHONE_HANDLING_VARIANCE_THRESHOLD) {
                    window.lastPhoneHandlingEventMs = now
                    return createUnsafeBehavior(
                        "Phone Handling",
                        handlingSamples.last(),
                        variance.toFloat(),
                        context
                    )
                }
            }
        }

        if (currentSpeedMps >= FATIGUE_MIN_SPEED_MPS &&
            now - window.lastFatigueEventMs >= FATIGUE_COOLDOWN_MS
        ) {
            val longGyroSamples = window.longRotationWindow.filter { it.sensorType == GYROSCOPE_TYPE }
            val fatigueSamples = recentSamples(longGyroSamples, FATIGUE_WINDOW_MS)
            val durationMs = computeWindowDurationMs(fatigueSamples)
            if (durationMs >= FATIGUE_MIN_WINDOW_MS) {
                val yawSeries = lowPassYawSeries(buildYawSeries(window, fatigueSamples), FATIGUE_YAW_CUTOFF_HZ)
                val reversalRate = computeYawReversalRate(yawSeries)
                val maxAbsYaw = computeMaxAbsYaw(yawSeries)
                val roughnessSamples = recentSamples(window.accelerometerWindow, ROUGHNESS_WINDOW_MS)
                val rqi = computeRqi(window, roughnessSamples) ?: 0.0
                val tripId = rotationWindow.last().tripId
                val tripStart = tripId?.let { window.tripStartTimestampByTrip[it] }
                val continuousMs = tripStart?.let { now - it } ?: 0L
                val fatigueTimeGate =
                    isCircadianDip(now) || continuousMs >= FATIGUE_CONTINUOUS_DRIVING_MS
                Log.d(
                    "NewUnsafeDrivingBehaviourAnalyser",
                    "Fatigue check reversalRate=$reversalRate maxYaw=$maxAbsYaw rqi=$rqi durationMs=$durationMs timeGate=$fatigueTimeGate"
                )
                if (fatigueTimeGate &&
                    rqi <= ROUGH_ROAD_RQI_THRESHOLD &&
                    reversalRate <= FATIGUE_REVERSAL_RATE_THRESHOLD_HZ &&
                    maxAbsYaw >= FATIGUE_YAW_MAG_THRESHOLD
                ) {
                    window.lastFatigueEventMs = now
                    return createUnsafeBehavior(
                        "Fatigue",
                        rotationWindow.last(),
                        maxAbsYaw.toFloat(),
                        context
                    )
                }
            }
        }

        if (currentSpeedMps < MIN_SWERVE_SPEED_MPS) {
            return null
        }
        if (now - window.lastSwervingEventMs < SWERVE_COOLDOWN_MS) {
            return null
        }
        val recentSamples = recentSamples(gyroSamples, SWERVE_MAX_DURATION_MS)
        val durationMs = computeWindowDurationMs(recentSamples)
        val yawRate = computeMaxYawRate(window, recentSamples)
        val yawSeries = buildYawSeries(window, recentSamples)
        val hasSwervePattern = hasSwerveSignature(yawSeries)
        Log.d(
            "NewUnsafeDrivingBehaviourAnalyser",
            "Swerving check yawRate=$yawRate durationMs=$durationMs pattern=$hasSwervePattern"
        )
        return if (yawRate > SWERVING_THRESHOLD &&
            durationMs <= SWERVE_MAX_DURATION_MS &&
            hasSwervePattern
        ) {
            window.lastSwervingEventMs = now
            Log.d("NewUnsafeDrivingBehaviourAnalyser", "Yaw rate $yawRate > $SWERVING_THRESHOLD => Swerving")
            val eventSample = recentSamples.lastOrNull() ?: rotationWindow.last()
            createUnsafeBehavior("Swerving", eventSample, yawRate.toFloat(), context)
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
        val limitFromMap = locationData?.speedLimit?.takeIf { it > 0.0 }
        val effectiveSpeedLimit = limitFromMap ?: inferContextualSpeedLimit(avgSpeed, locationData)
        if (limitFromMap == null) {
            Log.d(
                "NewUnsafeDrivingBehaviourAnalyser",
                "Speed limit missing; using contextual cap=$effectiveSpeedLimit"
            )
        }
        val overspeedThreshold = effectiveSpeedLimit * SPEED_TOLERANCE_RATIO
        Log.d("NewUnsafeDrivingBehaviourAnalyser", "Effective speed limit: $effectiveSpeedLimit, avgSpeed=$avgSpeed")
        val now = speedWindow.last().timestamp
        val tripId = speedWindow.last().tripId
        if (tripId != null) {
            val crashCandidate = window.crashCandidatesByTrip[tripId]
            if (crashCandidate != null) {
                val elapsed = now - crashCandidate.timestampMs
                if (elapsed >= CRASH_CONFIRMATION_DELAY_MS) {
                    if (avgSpeed <= CRASH_CONFIRM_SPEED_THRESHOLD_MPS) {
                        window.lastCrashEventMs = now
                        window.crashCandidatesByTrip.remove(tripId)
                        Log.d(
                            "NewUnsafeDrivingBehaviourAnalyser",
                            "Crash confirmed tripId=$tripId deltaV=${crashCandidate.deltaVMps}"
                        )
                        return createUnsafeBehavior(
                            "Crash Detected",
                            crashCandidate.sample,
                            crashCandidate.deltaVMps.toFloat(),
                            context
                        )
                    } else if (elapsed >= CRASH_CANDIDATE_TTL_MS) {
                        window.crashCandidatesByTrip.remove(tripId)
                        Log.d(
                            "NewUnsafeDrivingBehaviourAnalyser",
                            "Crash candidate expired tripId=$tripId speed=$avgSpeed"
                        )
                    }
                }
            }
        }

        val roughnessSamples = recentSamples(window.accelerometerWindow, ROUGHNESS_WINDOW_MS)
        val rqi = computeRqi(window, roughnessSamples)
        if (rqi != null &&
            rqi >= ROUGH_ROAD_RQI_THRESHOLD &&
            avgSpeed >= ROUGH_ROAD_SPEED_THRESHOLD_MPS &&
            now - window.lastRoughRoadEventMs >= ROUGH_ROAD_COOLDOWN_MS
        ) {
            window.lastRoughRoadEventMs = now
            Log.d(
                "NewUnsafeDrivingBehaviourAnalyser",
                "Rough road speeding detected rqi=$rqi speed=$avgSpeed"
            )
            return createUnsafeBehavior(
                "Rough Road Speeding",
                speedWindow.last(),
                avgSpeed.toFloat(),
                context
            )
        }
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

        data class SpeedSample(val speed: Double, val weight: Double, val location: LocationData)

        val samples = mutableListOf<SpeedSample>()
        val locationIds = speedWindow.mapNotNull { it.locationId }.distinct()
        for (locationId in locationIds) {
            val locationData = fetchLocationDataById(locationId) ?: continue
            val speed = locationData.speed ?: continue
            if (speed <= 0.0) continue
            val accuracy = locationData.accuracy
            if (accuracy != null && accuracy > GPS_ACCURACY_REJECT_M) {
                continue
            }
            val quality = resolveGpsQuality(locationData)
            val weight = qualityWeight(quality)
            samples.add(SpeedSample(speed = speed, weight = weight, location = locationData))
        }

        val accelSpeeds = if (samples.isEmpty()) {
            accelWindow.mapNotNull { calculateSpeedFromAccelerometer(it.values) }
        } else {
            emptyList()
        }

        val averageSpeed = if (samples.isNotEmpty()) {
            val totalWeight = samples.sumOf { it.weight }.coerceAtLeast(1.0)
            val weightedSum = samples.sumOf { it.speed * it.weight }
            (weightedSum / totalWeight).coerceAtLeast(0.0)
        } else if (accelSpeeds.isNotEmpty()) {
            accelSpeeds.average()
        } else {
            0.0
        }

        val locationData = samples
            .filter { it.location.speedLimit > 0.0 }
            .sortedWith(
                compareByDescending<SpeedSample> { it.weight }
                    .thenByDescending { it.location.timestamp }
            )
            .firstOrNull()
            ?.location

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
                accuracy = it.accuracy,
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
            "Harsh Cornering" -> {
                val excess = measuredValue - CORNERING_THRESHOLD
                (excess / MAX_CORNERING_EXCESS).coerceIn(0f, 1f)
            }
            "Aggressive Turn" -> {
                val excess = measuredValue - AGGRESSIVE_TURN_LATERAL_THRESHOLD
                (excess / MAX_AGGRESSIVE_TURN_EXCESS).coerceIn(0f, 1f)
            }
            "Aggressive Stop-and-Go" -> {
                val excess = measuredValue - AGGRESSIVE_STOP_GO_JERK_THRESHOLD.toFloat()
                (excess / MAX_AGGRESSIVE_STOP_GO_EXCESS).coerceIn(0f, 1f)
            }
            "Phone Handling" -> {
                val excess = measuredValue - PHONE_HANDLING_VARIANCE_THRESHOLD.toFloat()
                (excess / MAX_PHONE_HANDLING_EXCESS).coerceIn(0f, 1f)
            }
            "Fatigue" -> {
                val excess = measuredValue - FATIGUE_YAW_MAG_THRESHOLD.toFloat()
                (excess / MAX_FATIGUE_YAW_EXCESS).coerceIn(0f, 1f)
            }
            "Rough Road Speeding" -> {
                val excess = measuredValue - ROUGH_ROAD_SPEED_THRESHOLD_MPS.toFloat()
                (excess / MAX_ROUGH_ROAD_SPEED_EXCESS).coerceIn(0f, 1f)
            }
            "Crash Detected" -> {
                val excess = measuredValue - CRASH_DELTA_V_THRESHOLD_MPS.toFloat()
                (excess / MAX_CRASH_DELTA_V_EXCESS).coerceIn(0f, 1f)
            }
            "Speeding" -> {
                if (speedLimit == null || speedLimit <= 0f) {
                    0f
                } else {
                    val excess = measuredValue - speedLimit
                    (excess / MAX_SPEED_EXCESS).coerceIn(0f, 1f)
                }
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
        val timeInterval = SENSOR_SAMPLE_INTERVAL_S
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
        const val SPEED_TOLERANCE_RATIO = 1.10
        const val CORNERING_THRESHOLD = 4.9f // ~0.5g
        const val CORNERING_MIN_DURATION_MS = 1_000L
        const val CORNERING_COOLDOWN_MS = 3_000L

        const val SPEEDING_MIN_DURATION_MS = 20_000L
        const val SPEEDING_COOLDOWN_MS = 10_000L
        const val MIN_ACCELERATION_SPEED_MPS = 1.4
        const val MIN_SWERVE_SPEED_MPS = 5.56
        const val MIN_CORNERING_SPEED_MPS = 5.56
        const val MIN_AGGRESSIVE_TURN_SPEED_MPS = 5.56
        const val MIN_AGGRESSIVE_STOP_GO_SPEED_MPS = 4.17

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
        const val SWERVE_SIGN_EPS = 0.02

        const val AGGRESSIVE_TURN_LATERAL_THRESHOLD = 3.5f
        const val AGGRESSIVE_TURN_HEADING_DEG = 60.0
        const val AGGRESSIVE_TURN_WINDOW_MS = 2_000L
        const val AGGRESSIVE_TURN_COOLDOWN_MS = 5_000L
        const val AGGRESSIVE_STOP_GO_JERK_THRESHOLD = 10.0
        const val AGGRESSIVE_STOP_GO_COOLDOWN_MS = 5_000L

        const val PHONE_HANDLING_MIN_SPEED_MPS = 2.78
        const val PHONE_HANDLING_WINDOW_MS = 2_500L
        const val PHONE_HANDLING_MIN_SAMPLES = 20
        const val PHONE_HANDLING_VARIANCE_THRESHOLD = 0.15
        const val PHONE_HANDLING_COOLDOWN_MS = 10_000L

        const val FATIGUE_WINDOW_MS = 60_000L
        const val FATIGUE_MIN_WINDOW_MS = 30_000L
        const val FATIGUE_YAW_CUTOFF_HZ = 2.0
        const val FATIGUE_REVERSAL_RATE_THRESHOLD_HZ = 0.5
        const val FATIGUE_YAW_MAG_THRESHOLD = 0.15
        const val FATIGUE_MIN_SPEED_MPS = 11.11
        const val FATIGUE_COOLDOWN_MS = 600_000L
        const val FATIGUE_CONTINUOUS_DRIVING_MS = 16_200_000L

        const val MIN_ACCELERATION_THRESHOLD = 1.5f
        const val MIN_BRAKING_THRESHOLD = 1.5f

        const val MAX_ACCELERATION_EXCESS = 5.5f
        const val MAX_BRAKING_EXCESS = 5.5f
        const val MAX_SWERVING_EXCESS = 0.5f
        const val MAX_SPEED_EXCESS = 2.778f
        const val MAX_CORNERING_EXCESS = 4.0f
        const val MAX_AGGRESSIVE_TURN_EXCESS = 4.0f
        const val MAX_AGGRESSIVE_STOP_GO_EXCESS = 15.0f
        const val MAX_PHONE_HANDLING_EXCESS = 0.35f
        const val MAX_FATIGUE_YAW_EXCESS = 0.4f
        const val MAX_ROUGH_ROAD_SPEED_EXCESS = 8.33f
        const val MAX_CRASH_DELTA_V_EXCESS = 5.0f

        const val SENSOR_SAMPLE_INTERVAL_S = 0.02
        const val GRAVITY_MPS2 = 9.81
        const val GPS_ACCURACY_PRECISE_M = 50.0
        const val GPS_ACCURACY_APPROX_M = 100.0
        const val GPS_ACCURACY_REJECT_M = 200.0
        const val MIN_HEADING_SPEED_MPS = 2.78
        const val MAX_HEADING_ACCURACY_M = 100.0
        const val ROTATION_VECTOR_MAX_AGE_MS = 750L
        const val LOG_RATE_LIMIT_MS = 5_000L

        const val DEFAULT_URBAN_SPEED_LIMIT_MPS = 16.67
        const val DEFAULT_HIGHWAY_SPEED_LIMIT_MPS = 30.56
        const val HIGHWAY_SPEED_THRESHOLD_MPS = 22.22

        const val ROUGH_ROAD_RQI_THRESHOLD = 2.0
        const val ROUGH_ROAD_SPEED_THRESHOLD_MPS = 22.22
        const val ROUGH_ROAD_COOLDOWN_MS = 10_000L

        const val CRASH_ACCEL_THRESHOLD_MPS2 = 39.24
        const val CRASH_INTEGRATION_WINDOW_MS = 120L
        const val CRASH_FREEFALL_WINDOW_MS = 600L
        const val CRASH_FREEFALL_THRESHOLD_MPS2 = 1.0f
        const val CRASH_FREEFALL_MIN_DURATION_MS = 300L
        const val CRASH_DELTA_V_THRESHOLD_MPS = 1.94
        const val CRASH_CONFIRMATION_DELAY_MS = 30_000L
        const val CRASH_CONFIRM_SPEED_THRESHOLD_MPS = 1.39
        const val CRASH_CANDIDATE_TTL_MS = 60_000L
        const val CRASH_COOLDOWN_MS = 60_000L

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
