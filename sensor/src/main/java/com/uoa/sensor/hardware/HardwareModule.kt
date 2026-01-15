package com.uoa.sensor.hardware

import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.motion.DrivingStateManager
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareModule @Inject constructor(
    private val drivingStateManager: DrivingStateManager,
    private val locationManager: LocationManager,
    private val sensorRecordingManager: SensorRecordingManager
) {

    fun start() {
        drivingStateManager.startMonitoring()
    }

    fun stop() {
        drivingStateManager.stopMonitoring()
        locationManager.stopLocationUpdates()
        sensorRecordingManager.stopRecording()
    }

    fun startMovementDetection() {
        drivingStateManager.startMonitoring()
    }

    fun stopMovementDetection() {
        drivingStateManager.stopMonitoring()
        locationManager.stopLocationUpdates()
    }

    fun startDataCollection(tripId: UUID) {
        // Delegate to DrivingStateManager for logic/state
        drivingStateManager.startTrip(tripId)
        // Ensure location updates are active for recording
        locationManager.startLocationUpdates()
        // Start raw sensor recording
        sensorRecordingManager.startRecording(tripId)
    }

    fun stopDataCollection() {
        // Delegate to DrivingStateManager
        drivingStateManager.stopTrip()
        locationManager.stopLocationUpdates()
        // Stop raw sensor recording
        sensorRecordingManager.stopRecording()
    }

    fun getDrivingStateManager(): DrivingStateManager {
        return drivingStateManager
    }

    fun cleanup() {
        drivingStateManager.release()
        sensorRecordingManager.cleanup()
        locationManager.stopLocationUpdates()
    }

    fun currentTripIdFlow(): StateFlow<UUID?> {
        return drivingStateManager.currentTripId
    }
}
