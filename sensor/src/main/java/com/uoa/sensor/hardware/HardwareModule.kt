package com.uoa.sensor.hardware

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.sensor.utils.GetSensorTypeNameUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import com.uoa.sensor.utils.ProcessSensorData.processSensorData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareModule @Inject constructor(
    private val accelerometerSensor: AccelerometerSensor,
    private val gyroscopeSensor: GyroscopeSensor,
    private val rotationVectorSensor: RotationVectorSensor,
    private val magnetometerSensor: MagnetometerSensor,
    private val gravitySensor: GravitySensor,
    private val linearAccelerationSensor: LinearAccelerationSensor,
    private val locationBufferManager: LocationDataBufferManager,
    private val locationManager: LocationManager,
    private val sensorDataBufferManager: SensorDataBufferManager,
    private val motionDetector: MotionDetector,
    private val aiModelInputRepository: AIModelInputRepository,
    private val locationRepository: LocationRepository,
    private val context: Context,
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    private val rawSensorDataRepository: RawSensorDataRepository
) : MotionDetector.MotionListener {

    private val isCollecting = AtomicBoolean(false)
    private var currentTripId: UUID? = null
    private var rotationMatrix = FloatArray(9) { 0f }
    private val hardwareModuleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorEventListener: (Int, List<Float>, Int) -> Unit




       override fun onMotionDetected() {

           hardwareModuleScope.launch {
//               Log.d("HardwareModule", "Vehicle Motion Detected")
               sensorDataColStateRepository.updateVehicleMovementStatus(true)
           }

           if (!isCollecting.get()) {
               isCollecting.set(true)
               locationManager.startLocationUpdates()
               startSensorListeners()
               hardwareModuleScope.launch {
//                   Log.d("HardwareModule","Data Collection Has Started")
                   sensorDataColStateRepository.updateCollectionStatus(isCollecting.get())
               }
           }
    }

    override fun onMotionStopped() {
        hardwareModuleScope.launch {
//            Log.d("HardwareModule", "Vehicle Motion Stopped")
            sensorDataColStateRepository.updateVehicleMovementStatus(false)
        }
        if (isCollecting.get()) {
            stopSensorListeners()
            isCollecting.set(false)
            hardwareModuleScope.launch{
                sensorDataColStateRepository.updateCollectionStatus(isCollecting.get())
            }
//            Log.d("HardwareModule", "Data collection stopped.")
        }
    }

    fun startDataCollection(tripId: UUID) {
//        Log.d("HardwareModule", "Starting data collection for trip: $tripId")
        hardwareModuleScope.launch(Dispatchers.IO){
//            Log.d("HardwareModule", "Updating tripStartStatus to true")
                sensorDataColStateRepository.startTripStatus(true)
        }
        currentTripId = tripId
        startHybridMotionDetection()
//        Log.d("HardwareModule", "Data collection started for trip: $tripId")

    }

    fun stopDataCollection() {
//        Log.d("HardwareModule", "Stopping data collection for trip: $currentTripId")
        hardwareModuleScope.launch(Dispatchers.IO){
//            Log.d("HardwareModule", "Updating tripStartStatus to false")
                    sensorDataColStateRepository.startTripStatus(false)

        }
        clear()
    }

    // Start Sensor Listeners
    private fun startSensorListeners() {
        try {

            // Start all sensors
            accelerometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            gyroscopeSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            rotationVectorSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            magnetometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            gravitySensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            linearAccelerationSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)


        } catch (e: Exception) {
            Log.e("HardwareModule", "Error starting sensor listeners", e)
        }
    }

    private fun setupListeners() {



        sensorEventListener = fun(sensorType: Int, values: List<Float>, accuracy: Int) {
            val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)

            if (sensorTypeName == "Rotation Vector") {
                updateRotationMatrix(values.toFloatArray())
            } else {
                currentTripId?.let { tripId ->
                    val timestamp = Instant.now().toEpochMilli()
                    val validValues = values.map { if (it.isFinite()) it else 0f }

                    // Process sensor data and transform it based on rotation matrix
                    val processedValues = processSensorData(sensorType, validValues.toFloatArray(), rotationMatrix)

                    val rawSensorData = RawSensorData(
                        id = UUID.randomUUID(),
                        sensorType = sensorType,
                        sensorTypeName = sensorTypeName,
                        values = processedValues.toList(),
                        timestamp = timestamp,
                        date = Date(timestamp),
                        accuracy=accuracy,
                        locationId = locationBufferManager.getCurrentLocationId(),
                        tripId = tripId,
                        driverProfileId= PreferenceUtils.getDriverProfileId(context),
                        sync = false
                    )



                    if (isCollecting.get()) {
//                        Log.d("HardwareModule", "Attempting to save Sensor Data")
                        sensorDataBufferManager.addToSensorBuffer(rawSensorData)

//                        hardwareModuleScope.launch(Dispatchers.IO) {
//                            val locationId = locationBufferManager.getCurrentLocationId()
//                            if (locationId != null) {
//                                Log.d("HardwareModule", "Processing sensor data for AIModelInput in coroutine")
//                                val location = locationRepository.getLocationById(locationId)
//                                if (location != null) {
//                                    Log.d("HardwareModule", "Processing sensor data for AIModelInput in coroutine")
//                                     aiModelInputRepository.processDataForAIModelInputs(rawSensorData, location.toDomainModel(), tripId)
//
////                                    Update Raw Sensor and Location data after using them
//                                    val rawSensorDataCopy=rawSensorData.copy(processed = true)
//                                    rawSensorDataRepository.updateRawSensorData(rawSensorDataCopy.toEntity())
//
//                                    val locationDataCopy=location.copy(processed = true)
//                                    locationRepository.updateLocation(locationDataCopy)
//
//                                    Log.d("HardwareModule", "Sensor data processing for AIModelInput completed")
//                                } else {
//                                    Log.e("HardwareModule", "Location not found for ID: $locationId")
//                                }
//                            } else {
//                                Log.e("HardwareModule", "Current Location ID is null")
//                            }
//                        }

                    }
                    else{
                        Log.d("HardwareModule", "Error Data Collection is disabled")
                    }
                }
            }
        }

        try {
            accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
            rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)
            magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gravitySensor.whenSensorValueChangesListener(sensorEventListener)


        } catch (e: Exception) {
            Log.e("HardwareModule", "Error setting up sensor listeners", e)
        }
    }





    private fun updateRotationMatrix(rotationValues: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationValues)
    }

    private fun stopSensorListeners() {
        try {
            try {
                if (accelerometerSensor.doesSensorExist())
                    accelerometerSensor.stopListeningToSensor()
                else
                    Log.d("Sensor", "Acceleration Sensor Does not Exists")
                if (gyroscopeSensor.doesSensorExist())
                    gyroscopeSensor.stopListeningToSensor()
                else
                    Log.d("Sensor", "gyroscope Sensor Does not Exists")

                if (rotationVectorSensor.doesSensorExist())
                    rotationVectorSensor.stopListeningToSensor()
                else
                    Log.d("Sensor", "rotationVector Sensor Does not Exists")
                if (magnetometerSensor.doesSensorExist())
                    magnetometerSensor.stopListeningToSensor()
                else
                    Log.d("Sensor", "Magnetometer Sensor Does not Exists")

                if (gravitySensor.doesSensorExist())
                    gravitySensor.stopListeningToSensor()
                else
                    Log.d("Sensor", "Gravity Sensor Does not Exists")

            } catch (e: Exception) {
                Log.e("HardwareModule", "Error stopping sensor listeners", e)
            }
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error stopping sensor listeners", e)
        }
    }

    fun startHybridMotionDetection() {
        motionDetector.addMotionListener(this)
        motionDetector.startHybridMotionDetection()
    }

    private fun clear() {
        stopSensorListeners()
        locationManager.stopLocationUpdates()
        motionDetector.removeMotionListener(this)
//        hardwareModuleScope.cancel() // Cancel ongoing coroutines
        currentTripId = null
        isCollecting.set(false)
        hardwareModuleScope.launch(Dispatchers.IO){
            sensorDataColStateRepository.updateVehicleMovementStatus(false)
            sensorDataColStateRepository.updateCollectionStatus(false)
        }
    }

    init {
        // Register HardwareModule as a listener to motion events from MotionDetector
        setupListeners()
        motionDetector.addMotionListener(this)

//        startHybridMotionDetection() // Ensure hybrid motion detection starts at initialization
    }
}