package com.uoa.sensor.repository

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.uoa.core.model.Road
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.UUID
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import com.uoa.sensor.motion.DrivingStateManager

@Singleton
class SensorDataColStateRepository @Inject constructor(
    private val drivingStateStore: DrivingStateStore
) {

    private val MOVING_SPEED_THRESHOLD = 1.2          // m/s – anything above ~4.3 km/h implies motion
    private val VEHICLE_SPEED_THRESHOLD = 4.5         // m/s – ~16 km/h strongly suggests a vehicle
    private val VEHICLE_SPEED_LOWER_BOUND = 3.0       // m/s – light vehicle movement when paired with accel/label
    private val VEHICLE_ACCEL_THRESHOLD = 1.2         // m/s^2 – bursty acceleration typical for vehicles
    private val FOOT_SPEED_CUTOFF = 3.0               // m/s – classify walking/running below this as non-vehicle
    private val GPS_FRESHNESS_THRESHOLD = 30_000L     // ms - consider GPS stale if older than 30 seconds
    private val ACCEL_NOISE_THRESHOLD = 0.15          // m/s^2 – ignore tiny accel noise
    private val MAX_COMPUTED_SPEED = 60.0             // m/s – cap fallback speed (~216 km/h)
    private val COMPUTED_SPEED_DECAY = 0.90           // decay factor when no motion
    private val MAX_INTEGRATION_DT_SEC = 2.0          // seconds – avoid large integration jumps

    // Scope for internal background tasks (like the timer)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var tripStartTime = 0L
    private var lastMetricsPersistTime = 0L
    private val metricsPersistIntervalMs = 5_000L

    // Tracks whether data collection is active (e.g., sensors running)
    private val _collectionStatus = MutableStateFlow(false)
    val collectionStatus: StateFlow<Boolean> get() = _collectionStatus

    // Tracks whether a trip has started
    private val _tripStartStatus = MutableStateFlow(false)
    val tripStartStatus: StateFlow<Boolean> get() = _tripStartStatus

    // Tracks whether the vehicle is currently moving
    private val _isVehicleMoving = MutableStateFlow(false)
    val isVehicleMoving: StateFlow<Boolean> get() = _isVehicleMoving
    val vehicleMovementStatus: StateFlow<Boolean> get() = _isVehicleMoving

    private val _explicitVehicleSignal = MutableStateFlow(false)

    private val _linAcceleReading = mutableFloatStateOf(0f)
    val linAcceleReading: MutableState<Float> get()=_linAcceleReading

    private val _movementLabel = mutableStateOf("")
    val movementLabel: MutableState<String> get()=_movementLabel

    private val _movementStatus = MutableStateFlow(false)
    val movementStatus: StateFlow<Boolean> get()=_movementStatus

    private val _currentSpeedMps = MutableStateFlow(0.0)
    val currentSpeedMps: StateFlow<Double> get() = _currentSpeedMps

    private val _computedSpeedMps = MutableStateFlow(0.0)
    private val _fusedSpeedMps = MutableStateFlow(0.0)
    val fusedSpeedMps: StateFlow<Double> = _fusedSpeedMps.asStateFlow()
    private var computedSpeedEma = 0.0
    private val computedSpeedSmoothingAlpha = 0.2

    private var lastAccelTimestamp = 0L
    private var lastGpsTimestamp = 0L
    private val _isGpsStale = MutableStateFlow(true)
    val isGpsStale: StateFlow<Boolean> = _isGpsStale.asStateFlow()

    // --- Trip Duration ---
    private val _tripDuration = MutableStateFlow("00:00:00")
    val tripDuration: StateFlow<String> = _tripDuration.asStateFlow()

    // --- Location / road tracking ---
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> get() = _currentLocation

    private val _distanceTravelled = MutableStateFlow(0.0)
    val distanceTravelled: StateFlow<Double> get() = _distanceTravelled

    private val _pathPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val pathPoints: StateFlow<List<GeoPoint>> get() = _pathPoints

    private val _nearbyRoads = MutableStateFlow<List<Road>>(emptyList())
    val nearbyRoads: StateFlow<List<Road>> get() = _nearbyRoads

    private val _speedLimit = MutableStateFlow(0)
    val speedLimit: StateFlow<Int> get() = _speedLimit

    private val _currentTripId = MutableStateFlow<UUID?>(null)
    val currentTripId: StateFlow<UUID?> = _currentTripId.asStateFlow()

    // --- Driving state from DrivingStateManager ---
    private val _drivingState = MutableStateFlow(DrivingStateManager.DrivingState.IDLE)
    val drivingState: StateFlow<DrivingStateManager.DrivingState> = _drivingState.asStateFlow()

    private val _drivingVariance = MutableStateFlow(0.0)
    val drivingVariance: StateFlow<Double> = _drivingVariance.asStateFlow()

    private val _drivingSpeedMps = MutableStateFlow(0.0)
    val drivingSpeedMps: StateFlow<Double> = _drivingSpeedMps.asStateFlow()

    private val _drivingAccuracy = MutableStateFlow(0f)
    val drivingAccuracy: StateFlow<Float> = _drivingAccuracy.asStateFlow()

    private val _drivingLastUpdate = MutableStateFlow(0L)
    val drivingLastUpdate: StateFlow<Long> = _drivingLastUpdate.asStateFlow()

    init {
        observePersistedState()
    }


    /**
     * Update the data collection status
     */
    suspend fun updateCollectionStatus(status: Boolean) {
        _collectionStatus.emit(status)
        if (status) {
            val now = System.currentTimeMillis()
            startTimer(now)
            drivingStateStore.updateCollectionStatus(true, now)
        } else {
            stopTimer()
            drivingStateStore.updateCollectionStatus(false, 0L)
        }
    }

    private fun startTimer(startTimeMs: Long) {
        if (timerJob?.isActive == true) return
        tripStartTime = startTimeMs
        timerJob = repoScope.launch {
            while (isActive) {
                val duration = System.currentTimeMillis() - tripStartTime
                val hours = duration / 3600000
                val minutes = (duration % 3600000) / 60000
                val seconds = (duration % 60000) / 1000
                _tripDuration.emit("%02d:%02d:%02d".format(hours, minutes, seconds))
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        tripStartTime = 0L
    }

    /**
     * Update the movement type
     */
    suspend fun updateMovementType(movementType: String) {
        _movementLabel.value = movementType.lowercase(Locale.ROOT)
        recomputeMovementSignals()
    }

    /**
     * Update the movement status
     */
    suspend fun updateMovementStatus(ismoving: Boolean) {
        _movementStatus.value = ismoving
        recomputeMovementSignals()
    }

    /**
     * Update Linear Acceleration reading
     */
    suspend fun updateLinearAcceleration(linAcceleReading: Double) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val isGpsFresh = (currentTime - lastGpsTimestamp) < GPS_FRESHNESS_THRESHOLD
            val drivingState = _drivingState.value
            val accelMagnitude = linAcceleReading.absoluteValue
            val allowIntegration =
                drivingState == DrivingStateManager.DrivingState.RECORDING ||
                    drivingState == DrivingStateManager.DrivingState.VERIFYING ||
                    _movementStatus.value ||
                    accelMagnitude >= VEHICLE_ACCEL_THRESHOLD

            if (!isGpsFresh && drivingState == DrivingStateManager.DrivingState.IDLE) {
                computedSpeedEma = 0.0
                _computedSpeedMps.emit(0.0)
            } else if (lastAccelTimestamp > 0 && allowIntegration) {
                val deltaTime = ((currentTime - lastAccelTimestamp) / 1000.0)
                    .coerceAtMost(MAX_INTEGRATION_DT_SEC)
                val effectiveAccel = if (linAcceleReading.absoluteValue < ACCEL_NOISE_THRESHOLD) {
                    0.0
                } else {
                    linAcceleReading
                }
                val newSpeed = _computedSpeedMps.value + (effectiveAccel * deltaTime)
                val clamped = newSpeed.coerceIn(0.0, MAX_COMPUTED_SPEED)
                computedSpeedEma = if (computedSpeedEma == 0.0) {
                    clamped
                } else {
                    (computedSpeedSmoothingAlpha * clamped) +
                        ((1 - computedSpeedSmoothingAlpha) * computedSpeedEma)
                }
                _computedSpeedMps.emit(computedSpeedEma)
            } else {
                // Decay computed speed when no motion/integration is expected.
                computedSpeedEma *= COMPUTED_SPEED_DECAY
                if (computedSpeedEma < 0.2) computedSpeedEma = 0.0
                _computedSpeedMps.emit(computedSpeedEma)
            }
            lastAccelTimestamp = currentTime

            _linAcceleReading.floatValue = linAcceleReading.toFloat()
            recomputeMovementSignals()
        }
    }

    // Human-readable derived property
    val readableAcceleration: androidx.compose.runtime.State<String> =
        androidx.compose.runtime.derivedStateOf {
            _linAcceleReading.floatValue.toReadableAcceleration()
        }

    // Extension function to convert to readable value
    private fun Float.toReadableAcceleration(): String {
        val accelerationKmh = this * 3.6f
        val threshold = 0.5f
        return when {
            accelerationKmh > threshold -> "(+${"%.1f".format(accelerationKmh)} km/h per second)"
            accelerationKmh < -threshold -> "(${ "%.1f".format(accelerationKmh) } km/h per second)"
            else -> "Converting(km/h per second)..."
        }
    }

    /**
     * Update the vehicle movement status
     */
    suspend fun updateVehicleMovementStatus(isVehicle: Boolean) {
        _explicitVehicleSignal.emit(isVehicle)
        recomputeMovementSignals()

    }

    /**
     * Update last known device speed in meters/second from GPS.
     */
    suspend fun updateSpeed(speedMps: Double) {
        lastGpsTimestamp = System.currentTimeMillis()
        // Reset computed speed when we have a valid GPS reading (even if 0)
        _computedSpeedMps.emit(speedMps)
        computedSpeedEma = speedMps
        _currentSpeedMps.emit(speedMps)
        recomputeMovementSignals()
    }

    /**
     * Update the trip start (or end) status
     */
    suspend fun startTripStatus(tripStarted: Boolean) {
        _tripStartStatus.emit(tripStarted)
        drivingStateStore.updateTripStartStatus(tripStarted)
    }

    suspend fun updateCurrentTripId(tripId: UUID?) {
        val shouldResetPath = tripId != null && tripId != _currentTripId.value
        if (shouldResetPath) {
            _distanceTravelled.emit(0.0)
            _pathPoints.emit(emptyList())
            _currentLocation.emit(null)
        }
        _currentTripId.emit(tripId)
        drivingStateStore.updateCurrentTripId(tripId)
    }

    suspend fun updateDrivingState(state: DrivingStateManager.DrivingState) {
        _drivingState.emit(state)
        when (state) {
            DrivingStateManager.DrivingState.IDLE -> {
                _movementLabel.value = "stationary"
                _explicitVehicleSignal.emit(false)
                _movementStatus.value = false
            }
            DrivingStateManager.DrivingState.VERIFYING -> {
                _movementLabel.value = "verifying"
                _explicitVehicleSignal.emit(false)
                _movementStatus.value = true
            }
            DrivingStateManager.DrivingState.RECORDING -> {
                _movementLabel.value = "vehicle"
                _explicitVehicleSignal.emit(true)
                _movementStatus.value = true
            }
            DrivingStateManager.DrivingState.POTENTIAL_STOP -> {
                _movementLabel.value = "stopped"
                _explicitVehicleSignal.emit(true)
                _movementStatus.value = true
            }
        }
        recomputeMovementSignals()
        drivingStateStore.updateDrivingState(state.name)
    }

    suspend fun updateDrivingMetrics(variance: Double, speedMps: Double, accuracy: Float) {
        _drivingVariance.emit(variance)
        _drivingSpeedMps.emit(speedMps)
        _drivingAccuracy.emit(accuracy)
        val now = System.currentTimeMillis()
        _drivingLastUpdate.emit(now)
        if (now - lastMetricsPersistTime >= metricsPersistIntervalMs) {
            lastMetricsPersistTime = now
            drivingStateStore.updateDrivingMetrics(variance, speedMps, accuracy, now)
        }
    }

    // ------------------------------------------------------------
    // Location helpers
    // ------------------------------------------------------------
    fun updateLocation(
        point: GeoPoint,
        distanceDelta: Double,
        roads: List<Road>,
        speedLimit: Int
    ) {
        _currentLocation.value = point
        _distanceTravelled.value += distanceDelta
        _pathPoints.value = _pathPoints.value + point
        _nearbyRoads.value = roads
        _speedLimit.value = speedLimit
    }

    fun updateRoadContext(
        roads: List<Road>,
        speedLimit: Int
    ) {
        _nearbyRoads.value = roads
        _speedLimit.value = speedLimit
    }
    private fun recomputeMovementSignals() {
        val label = _movementLabel.value
        val gpsSpeed = _currentSpeedMps.value
        val computedSpeed = _computedSpeedMps.value
        val isGpsFresh = (System.currentTimeMillis() - lastGpsTimestamp) < GPS_FRESHNESS_THRESHOLD
        _isGpsStale.value = !isGpsFresh

        // Use GPS speed if fresh (even if 0), otherwise fallback to computed
        val finalSpeed = if (isGpsFresh) gpsSpeed else computedSpeed
        _fusedSpeedMps.value = finalSpeed

        val accel = _linAcceleReading.floatValue.toDouble().absoluteValue
        val movingBySpeed = finalSpeed >= MOVING_SPEED_THRESHOLD
        val movingByLabel = label == "walking" || label == "running" || label == "vehicle"

        val vehicleBySpeed = finalSpeed >= VEHICLE_SPEED_THRESHOLD
        val vehicleByLabel = label == "vehicle"
        val vehicleByAccel = accel >= VEHICLE_ACCEL_THRESHOLD && finalSpeed >= VEHICLE_SPEED_LOWER_BOUND
        val explicitVehicle = _explicitVehicleSignal.value
        val definitelyOnFoot = (label == "walking" || label == "running") && finalSpeed < FOOT_SPEED_CUTOFF

        val resolvedVehicle = if (definitelyOnFoot) {
            false
        } else {
            explicitVehicle || vehicleBySpeed || (vehicleByLabel && (vehicleByAccel || finalSpeed >= VEHICLE_SPEED_LOWER_BOUND))
        }

        _movementStatus.value = movingBySpeed || movingByLabel || explicitVehicle || resolvedVehicle
        _isVehicleMoving.value = resolvedVehicle
    }

    private fun observePersistedState() {
        repoScope.launch {
            drivingStateStore.snapshotFlow.collect { snapshot ->
                snapshot.drivingState?.let { stateName ->
                    val restoredState = runCatching {
                        DrivingStateManager.DrivingState.valueOf(stateName)
                    }.getOrNull()
                    if (restoredState != null && _drivingState.value != restoredState) {
                        applyDrivingState(restoredState)
                    }
                }
                snapshot.drivingVariance?.let { variance ->
                    if (_drivingVariance.value != variance) {
                        _drivingVariance.value = variance
                    }
                }
                snapshot.drivingSpeedMps?.let { speedMps ->
                    if (_drivingSpeedMps.value != speedMps) {
                        _drivingSpeedMps.value = speedMps
                    }
                }
                snapshot.drivingAccuracy?.let { accuracy ->
                    if (_drivingAccuracy.value != accuracy) {
                        _drivingAccuracy.value = accuracy
                    }
                }
                snapshot.drivingLastUpdate?.let { lastUpdate ->
                    if (_drivingLastUpdate.value != lastUpdate) {
                        _drivingLastUpdate.value = lastUpdate
                    }
                }
                snapshot.collectionStatus?.let { collection ->
                    if (_collectionStatus.value != collection) {
                        _collectionStatus.value = collection
                    }
                    if (!collection && timerJob?.isActive == true) {
                        stopTimer()
                    }
                }
                snapshot.tripStartStatus?.let { tripStarted ->
                    if (_tripStartStatus.value != tripStarted) {
                        _tripStartStatus.value = tripStarted
                    }
                }
                snapshot.currentTripId?.let { tripIdString ->
                    val tripId = runCatching { UUID.fromString(tripIdString) }.getOrNull()
                    if (_currentTripId.value != tripId) {
                        _currentTripId.value = tripId
                    }
                }
                snapshot.tripStartTime?.let { startTime ->
                    if (_collectionStatus.value && startTime > 0L && timerJob?.isActive != true) {
                        startTimer(startTime)
                    }
                }
            }
        }
    }

    private fun applyDrivingState(state: DrivingStateManager.DrivingState) {
        _drivingState.value = state
        when (state) {
            DrivingStateManager.DrivingState.IDLE -> {
                _movementLabel.value = "stationary"
                _explicitVehicleSignal.value = false
                _movementStatus.value = false
            }
            DrivingStateManager.DrivingState.VERIFYING -> {
                _movementLabel.value = "verifying"
                _explicitVehicleSignal.value = false
                _movementStatus.value = true
            }
            DrivingStateManager.DrivingState.RECORDING -> {
                _movementLabel.value = "vehicle"
                _explicitVehicleSignal.value = true
                _movementStatus.value = true
            }
            DrivingStateManager.DrivingState.POTENTIAL_STOP -> {
                _movementLabel.value = "stopped"
                _explicitVehicleSignal.value = true
                _movementStatus.value = true
            }
        }
        recomputeMovementSignals()
    }

}
