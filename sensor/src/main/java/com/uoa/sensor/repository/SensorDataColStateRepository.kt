package com.uoa.sensor.repository

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.uoa.core.model.Road
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class SensorDataColStateRepository @Inject constructor() {

    private val MOVING_SPEED_THRESHOLD = 1.2          // m/s – anything above ~4.3 km/h implies motion
    private val VEHICLE_SPEED_THRESHOLD = 4.5         // m/s – ~16 km/h strongly suggests a vehicle
    private val VEHICLE_SPEED_LOWER_BOUND = 3.0       // m/s – light vehicle movement when paired with accel/label
    private val VEHICLE_ACCEL_THRESHOLD = 1.2         // m/s^2 – bursty acceleration typical for vehicles
    private val FOOT_SPEED_CUTOFF = 3.0               // m/s – classify walking/running below this as non-vehicle

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


    /**
     * Update the data collection status
     */
    suspend fun updateCollectionStatus(status: Boolean) {
        _collectionStatus.emit(status)
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
        _currentSpeedMps.emit(speedMps)
        recomputeMovementSignals()
    }

    /**
     * Update the trip start (or end) status
     */
    suspend fun startTripStatus(tripStarted: Boolean) {
        _tripStartStatus.emit(tripStarted)
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
<<<<<<< Updated upstream

    private fun recomputeMovementSignals() {
        val label = _movementLabel.value
        val speed = _currentSpeedMps.value
        val accel = _linAcceleReading.floatValue.toDouble().absoluteValue
        val movingBySpeed = speed >= MOVING_SPEED_THRESHOLD
        val movingByLabel = label == "walking" || label == "running" || label == "vehicle"

        val vehicleBySpeed = speed >= VEHICLE_SPEED_THRESHOLD
        val vehicleByLabel = label == "vehicle"
        val vehicleByAccel = accel >= VEHICLE_ACCEL_THRESHOLD && speed >= VEHICLE_SPEED_LOWER_BOUND
        val explicitVehicle = _explicitVehicleSignal.value
        val definitelyOnFoot = (label == "walking" || label == "running") && speed < FOOT_SPEED_CUTOFF

        val resolvedVehicle = if (definitelyOnFoot) {
            false
        } else {
            explicitVehicle || vehicleBySpeed || (vehicleByLabel && (vehicleByAccel || speed >= VEHICLE_SPEED_LOWER_BOUND))
        }

        _movementStatus.value = movingBySpeed || movingByLabel || explicitVehicle || resolvedVehicle
        _isVehicleMoving.value = resolvedVehicle
    }
=======
>>>>>>> Stashed changes
}
