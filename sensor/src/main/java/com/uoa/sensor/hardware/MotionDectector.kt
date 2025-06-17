//package com.uoa.sensor.hardware
//
//import android.hardware.Sensor
//import android.hardware.SensorManager
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import com.uoa.sensor.hardware.base.SignificantMotionSensor
//import com.uoa.sensor.utils.ProcessSensorData.processSensorData
//import javax.inject.Inject
//import kotlin.math.pow
//import kotlin.math.sqrt
//
//class MotionDetector @Inject constructor(
//    private val significantMotionSensor: SignificantMotionSensor,
//    private val linearAccelerationSensor: LinearAccelerationSensor,
//    private val accelerometerSensor: AccelerometerSensor
//) {
//    interface MotionListener {
//        fun onMotionDetected()
//        fun onMotionStopped()
//    }
//
//    private val LINEAR_ACCELERATION_THRESHOLD = 0.8f // Threshold for vehicle movement
//    private val SIGNIFICANT_MOTION_WAIT_DURATION = 5000L // Wait duration for significant motion detection in milliseconds
//    private val DEBOUNCE_INTERVAL = 100000L // Debounce interval in milliseconds to avoid frequent state changes
//
//    private val motionListeners = mutableListOf<MotionListener>()
//    private val fallbackHandler = Handler(Looper.getMainLooper())
//    private val debounceHandler = Handler(Looper.getMainLooper())
//    private var isVehicleMoving = false
//    private var lastMotionChangeTimestamp = 0L
//
//    // Add and remove motion listeners
//    fun addMotionListener(listener: MotionListener) {
//        if (!motionListeners.contains(listener)) {
//            motionListeners.add(listener)
//        }
//    }
//
//    fun removeMotionListener(listener: MotionListener) {
//        motionListeners.remove(listener)
//    }
//
//    private fun notifyMotionDetected() {
//        for (listener in motionListeners) {
//            listener.onMotionDetected()
//        }
//    }
//
//    private fun notifyMotionStopped() {
//        for (listener in motionListeners) {
//            listener.onMotionStopped()
//        }
//    }
//
//    // Start hybrid motion detection
//    fun startHybridMotionDetection() {
//        try {
//            var significantMotionStarted = false
//
//            if (significantMotionSensor.doesSensorExist()) {
//                significantMotionStarted = significantMotionSensor.startListeningToSensor()
//                if (significantMotionStarted) {
//                    significantMotionSensor.setOnTriggerListener {
//                        handleMotionDetected()
//                    }
//                }
//            }
//
//            if (!significantMotionStarted) {
//                startFallbackDetection()
//            } else {
//                setupFallbackTimer()
//            }
//        } catch (e: Exception) {
//            Log.e("MotionDetector", "Error initializing hybrid motion detection", e)
//        }
//    }
//
//    // Fallback Detection
//    private fun startFallbackDetection() {
//        try {
//            if (linearAccelerationSensor.doesSensorExist()) {
//                linearAccelerationSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
//                linearAccelerationSensor.whenSensorValueChangesListener { _, values, _ ->
//                    checkMotion(processSensorData(Sensor.TYPE_ACCELEROMETER,values.toFloatArray(),floatArrayOf(0f,0f,0f)).toList())
//                }
//            } else if (accelerometerSensor.doesSensorExist()) {
//                accelerometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
//                accelerometerSensor.whenSensorValueChangesListener { _, values, _ ->
//                    checkMotion(processSensorData(Sensor.TYPE_ACCELEROMETER,values.toFloatArray(),floatArrayOf(0f,0f,0f)).toList())
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("MotionDetector", "Error in fallback motion detection", e)
//        }
//    }
//
//    // Setup fallback timer for significant motion sensor
//    private fun setupFallbackTimer() {
//        fallbackHandler.postDelayed({
//            if (!isVehicleMoving) {
//                startFallbackDetection()
//            }
//        }, SIGNIFICANT_MOTION_WAIT_DURATION)
//    }
//
//    private fun checkMotion(values: List<Float>) {
//        val magnitude = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
//        if (magnitude > LINEAR_ACCELERATION_THRESHOLD) {
//            handleMotionDetected()
//        } else {
//            handleMotionStopped()
//        }
//    }
//
//    // Handle motion detected with debounce logic
//    private fun handleMotionDetected() {
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastMotionChangeTimestamp > DEBOUNCE_INTERVAL) {
//            lastMotionChangeTimestamp = currentTime
//            if (!isVehicleMoving) {
//                isVehicleMoving = true
//                notifyMotionDetected()
//            }
//        } else {
//            debounceHandler.removeCallbacksAndMessages(null)
//            debounceHandler.postDelayed({
//                if (!isVehicleMoving) {
//                    isVehicleMoving = true
//                    notifyMotionDetected()
//                }
//            }, DEBOUNCE_INTERVAL)
//        }
//    }
//
//    // Handle motion stopped with debounce logic
//    private fun handleMotionStopped() {
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastMotionChangeTimestamp > DEBOUNCE_INTERVAL) {
//            lastMotionChangeTimestamp = currentTime
//            if (isVehicleMoving) {
//                isVehicleMoving = false
//                notifyMotionStopped()
//            }
//        } else {
//            debounceHandler.removeCallbacksAndMessages(null)
//            debounceHandler.postDelayed({
//                if (isVehicleMoving) {
//                    isVehicleMoving = false
//                    notifyMotionStopped()
//                }
//            }, DEBOUNCE_INTERVAL)
//        }
//    }
//}
//
//
