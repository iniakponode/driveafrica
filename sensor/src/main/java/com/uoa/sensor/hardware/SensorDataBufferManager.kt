package com.uoa.sensor.hardware

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.model.RawSensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorDataBufferManager @Inject constructor(
    private val rawSensorDataRepository: RawSensorDataRepository,
) {

    private val sensorDataBuffer = mutableListOf<RawSensorData>()
    private val bufferInsertInterval: Long = 5000  // Time interval to process the buffer (e.g., every 5 seconds)
    private val bufferLimit = 500  // Threshold limit for batch size

    private val bufferHandler = runCatching { Looper.getMainLooper() }
        .getOrNull()
        ?.let { Handler(it) }
    private var isFlushHandlerRunning = false  // Track handler state

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)
    private val repositoryMutex = Mutex()
    @Volatile private var isProcessing = false
    @Volatile private var processingJob: kotlinx.coroutines.Job? = null

    private fun safeLog(block: () -> Unit) {
        runCatching { block() }
    }

    init {
        startBufferFlushHandler()
    }

    /**
     * Adds sensor data to the in-memory buffer.
     * If it exceeds the bufferLimit, it triggers immediate processing.
     */
    fun addToSensorBuffer(rawSensorData: RawSensorData) {
        synchronized(sensorDataBuffer) {
            sensorDataBuffer.add(rawSensorData)
//            Log.d("BufferManager", "Successfully added new data to the buffer ${sensorDataBuffer.last()}.")
            if (sensorDataBuffer.size >= bufferLimit) {
                processAndStoreSensorData()
            }
        }
    }

    /**
     * Periodic flush: every bufferInsertInterval ms, we call processAndStoreSensorData().
     * This ensures we eventually write data even if the buffer doesn't reach bufferLimit.
     */
    private fun startBufferFlushHandler() {
        val handler = bufferHandler ?: run {
            safeLog {
                Log.w("SensorBufferManager", "Main looper unavailable; buffer flush handler disabled")
            }
            return
        }
        if (isFlushHandlerRunning) {
            safeLog { Log.w("SensorBufferManager", "Buffer flush handler already running") }
            return
        }
        isFlushHandlerRunning = true

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isFlushHandlerRunning) return  // Check if stopped
                processAndStoreSensorData()
                handler.postDelayed(this, bufferInsertInterval)
            }
        }, bufferInsertInterval)

        safeLog { Log.d("SensorBufferManager", "Buffer flush handler started") }
    }

    /**
     * Stop the buffer flush handler to prevent memory leaks
     */
    fun stopBufferFlushHandler() {
        isFlushHandlerRunning = false
        bufferHandler?.removeCallbacksAndMessages(null)
        safeLog { Log.d("SensorBufferManager", "Buffer flush handler stopped") }
    }

    /**
     * Clear the buffer contents
     */
    fun clearBuffer() {
        synchronized(sensorDataBuffer) {
            val size = sensorDataBuffer.size
            sensorDataBuffer.clear()
            safeLog { Log.d("SensorBufferManager", "Buffer cleared ($size items removed)") }
        }
    }

    /**
     * Complete cleanup - call this when shutting down
     */
    fun cleanup() {
        try {
            stopBufferFlushHandler()
            scopeJob.cancel()  // Cancel all coroutines
            clearBuffer()
            safeLog { Log.d("SensorBufferManager", "Complete cleanup finished") }
        } catch (e: Exception) {
            safeLog { Log.e("SensorBufferManager", "Error during cleanup", e) }
        }
    }

    /**
     * Asynchronous flush:
     *  - Copies the buffer contents,
     *  - Clears the buffer,
     *  - Launches a coroutine to insert data in the background.
     *
     * IMPORTANT: Because this uses 'launch', the calling thread won't block.
     *            If you want to *wait* until insertion finishes, see flushBufferToDatabase().
     */
    fun processAndStoreSensorData() {
        if (isProcessing) return
        val bufferCopy: List<RawSensorData>
        synchronized(sensorDataBuffer) {
            if (sensorDataBuffer.isEmpty()) return
            bufferCopy = sensorDataBuffer.toList()
            sensorDataBuffer.clear()
        }

        isProcessing = true
        processingJob = scope.launch {
            try {
                repositoryMutex.withLock {
                    // Just call the repository method (transaction logic is inside the repository)
                    safeLog {
                        Log.d("SensorBufferManager", "Processing buffer batch, count=${bufferCopy.size}.")
                    }
                    rawSensorDataRepository.processAndStoreSensorData(bufferCopy)
                    safeLog { Log.d("SensorBufferManager", "Data processed successfully:.") }
                }
            } catch (e: Exception) {
                safeLog { Log.e("SensorBufferManager", "Error in processAndStoreSensorData", e) }
            } finally {
                isProcessing = false
                processingJob = null
            }
        }
    }

    /**
     * A SUSPENDING version that ensures the buffer is fully written before returning.
     * Useful when you want to finalize or guarantee data is persisted (e.g. end of a trip).
     */
    private suspend fun processAndStoreSensorDataSynchronous() {
        val bufferCopy: List<RawSensorData>
        // Copy & clear the buffer
        synchronized(sensorDataBuffer) {
            if (sensorDataBuffer.isEmpty()) return
            bufferCopy = sensorDataBuffer.toList()
            sensorDataBuffer.clear()
        }

        // Insert data on IO dispatcher *and wait* for completion
        repositoryMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    isProcessing = true
                    rawSensorDataRepository.processAndStoreSensorData(bufferCopy)
                    safeLog { Log.d("SensorBufferManager", "Data processed successfully (sync).") }
                } catch (e: Exception) {
                    safeLog { Log.e("SensorBufferManager", "Error in processAndStoreSensorDataSync", e) }
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    /**
     * Forces an immediate, synchronous flush of any remaining data in the buffer
     * and waits for it to finish before returning.
     */
    suspend fun flushBufferToDatabase() {
        processingJob?.join()
        processAndStoreSensorDataSynchronous()
    }

    /**
     * Called at the end of a trip or other scenario where you want to ensure
     * all sensor data is permanently stored, plus any additional "finalization" tasks.
     */
    suspend fun finalizeCurrentTrip(tripId: UUID) {
        // 1) Flush remaining buffer data to the database
        flushBufferToDatabase()

        // 2) Perform any other final tasks, such as marking the trip completed:
        withContext(Dispatchers.IO) {
            try {
                // If you have a repository method to finalize a trip, call it here:
                // e.g.: rawSensorDataRepository.markTripAsFinished(tripId)
                safeLog { Log.d("SensorDataBufferManager", "Trip $tripId finalized successfully.") }
            } catch (e: Exception) {
                safeLog { Log.e("SensorDataBufferManager", "Error finalizing trip $tripId", e) }
            }
        }
    }



    /**
     * Used to stop the buffer's background flushing if necessary (e.g. shutting down).
     */
    fun clear() {
        bufferHandler?.removeCallbacksAndMessages(null)
    }
}
