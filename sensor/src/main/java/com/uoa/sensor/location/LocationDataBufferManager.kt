package com.uoa.sensor.location

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDataBufferManager @Inject constructor(
    private val locationRepositoryImpl: LocationRepository
) {
    private val locationDataBuffer = mutableListOf<LocationData>()
    private val bufferInsertInterval: Long = 5000  // Interval for batch insert
    private val locationDataBufferLimit = 50
    private val handler = Handler(Looper.getMainLooper())
    private var currentLocationId: UUID? = null

    init {
        startBufferInsertHandler()
    }

    fun addLocationData(locationData: LocationData) {
        locationDataBuffer.add(locationData)
        if (locationDataBuffer.size >= locationDataBufferLimit) {
            processAndStoreLocationData()
        }
    }

    private fun processAndStoreLocationData() {
        if (locationDataBuffer.isEmpty()) return

        val bufferCopy = locationDataBuffer.toList()
        currentLocationId = bufferCopy.lastOrNull()?.id
        locationDataBuffer.clear()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                locationRepositoryImpl.insertLocationBatch(bufferCopy.map { it.toEntity() })
            } catch (e: Exception) {
                Log.e("LocationDataBufferManager", "Error processing and storing location data", e)
            }
        }
    }

    private fun startBufferInsertHandler() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                processAndStoreLocationData()
                handler.postDelayed(this, bufferInsertInterval)
            }
        }, bufferInsertInterval)
    }

    fun stopBufferHandler() {
        handler.removeCallbacksAndMessages(null)
    }

    fun getCurrentLocationId(): UUID? {
        return currentLocationId
    }
}