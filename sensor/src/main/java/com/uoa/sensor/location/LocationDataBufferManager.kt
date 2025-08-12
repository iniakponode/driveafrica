package com.uoa.sensor.location

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.utils.toEntity
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
class LocationDataBufferManager @Inject constructor(
    private val locationRepositoryImpl: LocationRepository
) {
    private val locationDataBuffer = mutableListOf<LocationData>()
    private val bufferInsertInterval: Long = 5000  // e.g., flush every 5 seconds
    private val locationDataBufferLimit = 50

    private val handler = Handler(Looper.getMainLooper())
    private var currentLocationId: UUID? = null

    // Flag to prevent concurrent inserts
    @Volatile
    private var isInsertingData = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repositoryMutex = Mutex()

    init {
        startBufferInsertHandler()
    }

    /**
     * Adds a new LocationData entry to the buffer.
     * If the buffer exceeds locationDataBufferLimit,
     * we trigger an async insertion (processAndStoreLocationData).
     */
    fun addLocationData(locationData: LocationData) {
        synchronized(locationDataBuffer) {
            locationDataBuffer.add(locationData)
        }

        if (locationDataBuffer.size >= locationDataBufferLimit) {
            processAndStoreLocationData()
        }
    }

    /**
     * Periodic flush handler that runs every bufferInsertInterval ms,
     * calling processAndStoreLocationData() if the buffer has data.
     */
    private fun startBufferInsertHandler() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                processAndStoreLocationData()
                handler.postDelayed(this, bufferInsertInterval)
            }
        }, bufferInsertInterval)
    }

    /**
     * Async approach: copies the buffer, clears it, then inserts the batch in a coroutine.
     * This does NOT block the caller. If you want a blocking flush, see flushBufferToDatabase().
     */
    fun processAndStoreLocationData() {
        // If already inserting or buffer is empty, do nothing
        if (isInsertingData || locationDataBuffer.isEmpty()) return

        val bufferCopy: List<LocationData>
        synchronized(locationDataBuffer) {
            bufferCopy = locationDataBuffer.toList()
            locationDataBuffer.clear()
        }

        isInsertingData = true

        scope.launch {
            try {
                repositoryMutex.withLock {
                    locationRepositoryImpl.insertLocationBatch(bufferCopy.map { it.toEntity() })
                    // The newest location ID is the last one we added:
                    val lastId = bufferCopy.lastOrNull()?.id
                    if (lastId != null) {
                        currentLocationId = lastId
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationDataBufferManager", "Error storing location data", e)
            } finally {
                isInsertingData = false
            }
        }
    }

    /**
     * Force a synchronous flush:
     *  1. We gather any pending data from the buffer,
     *  2. Insert on Dispatchers.IO, blocking until done,
     *  3. Update currentLocationId accordingly,
     *  4. Return after the data is fully persisted.
     */
    suspend fun flushBufferToDatabase() {
        // If there's nothing to insert, just return
        if (locationDataBuffer.isEmpty() && !isInsertingData) return

        val bufferCopy: List<LocationData>
        synchronized(locationDataBuffer) {
            bufferCopy = locationDataBuffer.toList()
            locationDataBuffer.clear()
        }

        isInsertingData = true
        repositoryMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    locationRepositoryImpl.insertLocationBatch(bufferCopy.map { it.toEntity() })
                    val lastId = bufferCopy.lastOrNull()?.id
                    if (lastId != null) {
                        currentLocationId = lastId
                    }
                } catch (e: Exception) {
                    Log.e("LocationDataBufferManager", "Error in flushBufferToDatabase", e)
                } finally {
                    isInsertingData = false
                }
            }
        }
    }

    /**
     * Called when shutting down or stopping location updates,
     * to remove scheduled flush callbacks from the handler.
     */
    fun stopBufferHandler() {
        handler.removeCallbacksAndMessages(null)
    }

    fun getCurrentLocationId(): UUID? {
        return currentLocationId
    }
}

