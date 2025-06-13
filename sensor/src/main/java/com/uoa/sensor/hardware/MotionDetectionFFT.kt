// MotionDetectionFFT.kt
package com.uoa.sensor.hardware

import android.hardware.SensorManager
import android.util.Log
import com.uoa.core.database.entities.FFTFeatureDao
import com.uoa.core.database.entities.FFTFeatureEntity
import com.uoa.sensor.hardware.base.SignificantMotionSensor
import com.uoa.sensor.repository.SensorDataColStateRepository
import kotlinx.coroutines.*
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class MotionDetectionFFT @Inject constructor(
    private val significantMotionSensor: SignificantMotionSensor,
    private val linearSensor: LinearAccelerationSensor,
    private val accelSensor: AccelerometerSensor,
    private val fftFeatureDao: FFTFeatureDao,
    private val stateRepo: SensorDataColStateRepository
) {
    interface MotionListener { fun onMotionDetected(); fun onMotionStopped() }

    private val TAG = "MotionDetectionFFT"
    private val SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // —— Tunables ——
    private val SIGNIFICANT_WAIT = 5_000L
    private val INACTIVITY_TIMEOUT = 300_000L
    private val RESUME_DELAY = 300_000L
    private val THROTTLE_MS = 500L           // at most 2 calls/sec
    private val DEBOUNCE_MS = 2_000L         // only 2s of stable opposite state

    private var lastProcessTime = 0L
    private var lastMotionChange = 0L
    private var lastMotionTimestamp = System.currentTimeMillis()
    private var bufferFull = false

    private val fftClassifier = MotionFFTClassifier(sampleRate = 10, bufferSize = 64)
    private var fallbackJob: Job? = null
    private var debounceJob: Job? = null
    private var inactivityJob: Job? = null

    // state flags
    private var isWalking = false
    private var isRunning = false
    private var isVehicle = false
    private var isStationary=false

    private val listeners = mutableSetOf<MotionListener>()

    fun addMotionListener(l: MotionListener)    = listeners.add(l)
    fun removeMotionListener(l: MotionListener) = listeners.remove(l)

    fun startHybridMotionDetection() {
        SCOPE.launch {
            var sigStarted = false
            try {
                if (significantMotionSensor.doesSensorExist()) {
                    sigStarted = significantMotionSensor.startListeningToSensor()
                    if (sigStarted) {
                        significantMotionSensor.setOnTriggerListener {
                            onVehicleDetected() // true positive via HW sensor
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "sig-sensor init failed", e)
            }
            if (!sigStarted) {
                // no HW fallback → start right away
                startFallback()
            } else {
                // wait 5s for SIG, then fallback
                fallbackJob?.cancel()
                fallbackJob = SCOPE.launch {
                    delay(SIGNIFICANT_WAIT)
                    if (!isVehicle) startFallback()
                }
            }
        }
    }

    fun stopHybridMotionDetection() {
        SCOPE.coroutineContext.cancelChildren()
        try {
            significantMotionSensor.stopListeningToSensor()
            linearSensor.stopListeningToSensor()
            accelSensor.stopListeningToSensor()
        } catch (_: Exception) { }
    }

    private fun startFallback() {
        Log.d(TAG, "Starting fallback FFT detection")
        lastMotionTimestamp = System.currentTimeMillis()

        val sensor = if (linearSensor.doesSensorExist()) linearSensor else accelSensor
        sensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
        sensor.whenSensorValueChangesListener { _, values, _ ->
            val now = System.currentTimeMillis()
            if (now - lastProcessTime < THROTTLE_MS) return@whenSensorValueChangesListener
            lastProcessTime = now

            // compute magnitude…
            val mag = run {
                val x2 = values.getOrNull(0)?.pow(2) ?: 0f
                val y2 = values.getOrNull(1)?.pow(2) ?: 0f
                val z2 = values.getOrNull(2)?.pow(2) ?: 0f
                sqrt((x2 + y2 + z2).toDouble())
            }

            if (fftClassifier.addSample(mag)) {
                val result = fftClassifier.classify()
                Log.d(TAG, "Classification: ${result.label} | Energy: ${"%.1f".format(result.energy)} | Freq: ${"%.2f".format(result.dominantFrequency)} | Entropy: ${"%.2f".format(result.entropy)}")

                // persist
                SCOPE.launch {
                    fftFeatureDao.insert(
                        FFTFeatureEntity(
                            timestamp         = System.currentTimeMillis(),
                            label             = result.label,
                            energy            = result.energy,
                            dominantFrequency = result.dominantFrequency,
                            entropy           = result.entropy
                        )
                    )
                    stateRepo.updateMovementType(result.label)
                }

                // dispatch strictly by label
                when (result.label) {
                    "vehicle"    -> onVehicleDetected()
                    "walking"    -> onWalkingDetected()
                    "running"    -> onRunningDetected()
                    "stationary" -> onVehicleStopped()
                    "unknown"    -> {
                        Log.d(TAG, "FFT → unknown → no state change")
                    }
                    else -> {
                        Log.w(TAG, "FFT → unexpected label '${result.label}'")
                    }
                }
            }

            // always update raw mag if needed
            SCOPE.launch { stateRepo.updateLinearAcceleration(mag) }
            scheduleInactivityCheck()
        }
    }

    private fun scheduleInactivityCheck() {
        inactivityJob?.cancel()
        inactivityJob = SCOPE.launch {
            delay(INACTIVITY_TIMEOUT)
            if (System.currentTimeMillis() - lastMotionTimestamp >= INACTIVITY_TIMEOUT) {
                Log.d(TAG, "Inactivity → pausing fallback")
                linearSensor.stopListeningToSensor()
                accelSensor.stopListeningToSensor()
                // resume later
                delay(RESUME_DELAY)
                startFallback()
            }
        }
    }

    private fun onVehicleDetected() {
        val now = System.currentTimeMillis()
        // reset inactivity timer
        lastMotionTimestamp = now

        // If we’re already in vehicle mode, do nothing
        if (isVehicle) return

        // Debounce the transition
        if (now - lastMotionChange > DEBOUNCE_MS) {
            transitionToVehicle(now)
        } else {
            debounceJob?.cancel()
            debounceJob = SCOPE.launch {
                delay(DEBOUNCE_MS)
                transitionToVehicle(System.currentTimeMillis())
            }
        }

    }

//    private fun onVehicleStopped() {
//        val now = System.currentTimeMillis()
//        lastMotionTimestamp = now
//        if (now - lastMotionChange > DEBOUNCE_MS) {
//            lastMotionChange = now
//            if (isVehicle) {
//                isVehicle = false
//                Log.d(TAG, "↘ VEHICLE STOPPED")
//                listeners.forEach { it.onMotionStopped() }
//            }
//        } else {
//            debounceJob?.cancel()
//            debounceJob = SCOPE.launch {
//                delay(DEBOUNCE_MS)
//                if (isVehicle) {
//                    isVehicle = false
//                    Log.d(TAG, "↘ VEHICLE STOPPED (debounce)")
//                    listeners.forEach { it.onMotionStopped() }
//                }
//            }
//        }
//    }

    private fun onWalkingDetected() {
        val now = System.currentTimeMillis()
        // reset inactivity timer
        lastMotionTimestamp = now

        // If we’re already in walking mode, do nothing
        if (isWalking) return

        // Debounce the transition
        if (now - lastMotionChange > DEBOUNCE_MS) {
            transitionToWalking(now)
        } else {
            debounceJob?.cancel()
            debounceJob = SCOPE.launch {
                delay(DEBOUNCE_MS)
                transitionToWalking(System.currentTimeMillis())
            }
        }
    }


    private fun onRunningDetected() {
        val now = System.currentTimeMillis()
        lastMotionTimestamp = now

        if (isRunning) return

        if (now - lastMotionChange > DEBOUNCE_MS) {
            transitionToRunning(now)
        } else {
            debounceJob?.cancel()
            debounceJob = SCOPE.launch {
                delay(DEBOUNCE_MS)
                transitionToRunning(System.currentTimeMillis())
            }
        }
    }

    private fun onVehicleStopped() {
        val now = System.currentTimeMillis()
        lastMotionTimestamp = now

        if (isStationary) return

        if (now - lastMotionChange > DEBOUNCE_MS) {
            transitionToStationary(now)
        } else {
            debounceJob?.cancel()
            debounceJob = SCOPE.launch {
                delay(DEBOUNCE_MS)
                transitionToStationary(System.currentTimeMillis())
            }
        }
    }

    // Shared transition helpers
    private fun transitionToWalking(now: Long) {
        lastMotionChange = now
        isWalking = true
        isRunning = false
        isVehicle = false
        isStationary=false

        Log.d(TAG, "↗ WALKING STARTED")
        // Notify generic “motion detected”
        listeners.forEach { it.onMotionDetected() }
        // Persist the type
        SCOPE.launch {
            stateRepo.updateMovementType("walking")
            stateRepo.updateMovementStatus(true)
        }
    }

    private fun transitionToRunning(now: Long) {
        lastMotionChange = now
        isRunning = true
        isWalking = false
        isVehicle = false
        isStationary=false

        Log.d(TAG, "↗ RUNNING STARTED")
        listeners.forEach { it.onMotionDetected() }
        SCOPE.launch {
            stateRepo.updateMovementType("running")
            stateRepo.updateMovementStatus(true)
        }
    }
    private fun transitionToVehicle(now: Long) {
        lastMotionChange = now
        isRunning = false
        isWalking = false
        isStationary=false
        isVehicle = true

        Log.d(TAG, "↗ Vehicle STARTED")
        listeners.forEach { it.onMotionDetected() }
        SCOPE.launch {
            stateRepo.updateMovementType("vehicle")
            stateRepo.updateMovementStatus(true)
            stateRepo.updateVehicleMovementStatus(true)
        }
    }
    private fun transitionToStationary(now: Long) {
        lastMotionChange = now
        isRunning = false
        isWalking = false
        isVehicle = false
        isStationary=true

        Log.d(TAG, "↗ STATIONARY STARTED")
        listeners.forEach { it.onMotionStopped() }
        SCOPE.launch {
            stateRepo.updateMovementStatus(false)
            stateRepo.updateMovementType("stationary") }
    }

}