package com.uoa.core.database.repository

import android.content.Context
import android.hardware.Sensor
import android.util.Log
import androidx.room.withTransaction
import com.uoa.core.Sdadb
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.entities.TripFeatureStateEntity
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import java.util.LinkedHashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessAndStoreSensorData @Inject constructor(
    private val rawSensorDataDao: RawSensorDataDao,
    private val appDatabase: Sdadb,
    private val context: Context,
    private val unsafeDrivingAnalyser: NewUnsafeDrivingBehaviourAnalyser,
    private val aiModelInputRepository: AIModelInputRepository,
    private val locationDao: LocationDao,
    private val unsafeBehaviourDao: UnsafeBehaviourDao,
    private val tripFeatureStateDao: TripFeatureStateDao
) {
    suspend fun processAndStoreSensorData(bufferCopy: List<RawSensorData>) {
        val TAG = "SENSORDATAPROCESSING"
        withContext(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    // 1) Insert raw data in bulk
                    Log.d(TAG, "Inserting raw sensor data batch, count=${bufferCopy.size}.")
                    rawSensorDataDao.insertRawSensorDataBatch(bufferCopy.map { it.toEntity() })
                }
                Log.d(TAG, "Raw sensor data batch persisted.")
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting raw sensor data batch: ${e.message}", e)
                throw e
            }

            try {
                updateTripFeatureState(bufferCopy)
            } catch (e: Exception) {
                Log.e(TAG, "Trip feature state update failed: ${e.message}", e)
            }

            // 2) Analyze data for unsafe behaviors (do not rollback raw insert on failures)
            val validItems = bufferCopy.filter { it.locationId != null }
            val invalidItems = bufferCopy.size - validItems.size
            if (invalidItems > 0) {
                Log.w(TAG, "Skipping $invalidItems items in this buffer copy because locationId is null.")
                bufferCopy.filter { it.locationId == null }.forEach { item ->
                    rawSensorDataDao.updateRawSensorData(item.copy(processed = true).toEntity())
                }
            }

            try {
                Log.d(TAG, "Analyzing data for unsafe behaviors...")
                val sensorDataFlow = validItems.asFlow().map { it.toEntity() }
                unsafeDrivingAnalyser.analyze(sensorDataFlow, context).collect { unsafeBehaviour ->
                    Log.d(TAG, "Unsafe behaviour detected: ${unsafeBehaviour.behaviorType}")
                    unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour.toEntity())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unsafe behaviour analysis failed: ${e.message}", e)
            }

            // 3) AI Model Processing and Mark processed (best-effort)
            Log.d(TAG, "Starting AI model processing for each record...")
            validItems.forEachIndexed { index, rawSensorData ->
                val locationId = rawSensorData.locationId
                val tripId = rawSensorData.tripId

                if (locationId == null) {
                    Log.w(TAG, "Skipping item index=$index; locationId is null.")
                    rawSensorDataDao.updateRawSensorData(rawSensorData.copy(processed = true).toEntity())
                    return@forEachIndexed
                }

                val locationEntity = locationDao.getLocationById(locationId)
                if (locationEntity == null) {
                    Log.w(TAG, "Skipping rawSensorData ID=${rawSensorData.id}; location not found for ID=$locationId.")
                    rawSensorDataDao.updateRawSensorData(rawSensorData.copy(processed = true).toEntity())
                    return@forEachIndexed
                }

                try {
                    if (tripId == null) {
                        Log.w(TAG, "Skipping rawSensorData ID=${rawSensorData.id}; tripId is null.")
                        rawSensorDataDao.updateRawSensorData(rawSensorData.copy(processed = true).toEntity())
                        return@forEachIndexed
                    }
                    Log.d(TAG, "Processing AIModelInput for rawSensorData ID=${rawSensorData.id}, location ID=$locationId.")
                    aiModelInputRepository.processDataForAIModelInputs(
                        sensorData = rawSensorData,
                        location = locationEntity.toDomainModel(),
                        tripId = tripId
                    )

                    val updatedRaw = rawSensorData.copy(processed = true)
                    rawSensorDataDao.updateRawSensorData(updatedRaw.toEntity())

                    val updatedLocation = locationEntity.copy(processed = true)
                    locationDao.updateLocation(updatedLocation)

                    Log.d(TAG, "Marked rawSensorData ID=${rawSensorData.id} and location ID=$locationId as processed.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing AIModelInput for rawSensorData ID=${rawSensorData.id}: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun updateTripFeatureState(bufferCopy: List<RawSensorData>) {
        if (bufferCopy.isEmpty()) return

        val itemsByTrip = bufferCopy
            .asSequence()
            .filter { it.tripId != null }
            .groupBy({ it.tripId!! }, { it })

        for ((tripId, sensors) in itemsByTrip) {
            val existing = tripFeatureStateDao.getByTripId(tripId)
            var state = existing ?: TripFeatureStateEntity(
                tripId = tripId,
                driverProfileId = sensors.firstNotNullOfOrNull { it.driverProfileId }
            )

            val accelSamples = sensors
                .asSequence()
                .filter {
                    it.sensorType == Sensor.TYPE_ACCELEROMETER
                }
                .mapNotNull { sensor ->
                    sensor.values.getOrNull(1)?.takeIf { it.isFinite() }?.toDouble()
                }
                .toList()

            if (accelSamples.isNotEmpty()) {
                var accelCount = state.accelCount
                var accelMean = state.accelMean
                accelSamples.forEach { value ->
                    val updated = updateMean(accelCount, accelMean, value)
                    accelCount = updated.first
                    accelMean = updated.second
                }
                state = state.copy(accelCount = accelCount, accelMean = accelMean)
            }

            val latestSensorTimestamp = sensors.maxOfOrNull { it.timestamp }
            state = state.copy(
                lastSensorTimestamp = maxOf(state.lastSensorTimestamp ?: 0L, latestSensorTimestamp ?: 0L)
                    .takeIf { it > 0L } ?: state.lastSensorTimestamp,
                driverProfileId = state.driverProfileId ?: sensors.firstNotNullOfOrNull { it.driverProfileId }
            )

            val locationIds = LinkedHashSet(
                sensors.sortedBy { it.timestamp }.mapNotNull { it.locationId }
            )
            if (locationIds.isNotEmpty()) {
                val locations = locationDao.getLocationsByIds(locationIds.toList())
                val locationsById = locations.associateBy { it.id }

                var speedStats = RunningStats(state.speedCount, state.speedMean, state.speedM2)
                var courseStats = RunningStats(state.courseCount, state.courseMean, state.courseM2)
                var lastLocationId = state.lastLocationId
                var lastLatitude = state.lastLatitude
                var lastLongitude = state.lastLongitude
                var lastLocationTimestamp = state.lastLocationTimestamp

                for (locationId in locationIds) {
                    if (locationId == lastLocationId) continue
                    val location = locationsById[locationId] ?: continue

                    val speedValue = location.speed.toDouble()
                    if (speedValue.isFinite() && speedValue >= 0.0) {
                        speedStats = updateRunningStats(speedStats, speedValue)
                    }

                    val timestamp = location.timestamp
                    val isChronological = lastLocationTimestamp == null || timestamp >= lastLocationTimestamp
                    if (lastLatitude != null && lastLongitude != null && isChronological) {
                        val bearing = calculateBearing(lastLatitude, lastLongitude, location.latitude, location.longitude)
                        if (bearing.isFinite()) {
                            courseStats = updateRunningStats(courseStats, bearing)
                        }
                    }

                    if (isChronological) {
                        lastLatitude = location.latitude
                        lastLongitude = location.longitude
                        lastLocationTimestamp = timestamp
                        lastLocationId = locationId
                    }
                }

                state = state.copy(
                    speedCount = speedStats.count,
                    speedMean = speedStats.mean,
                    speedM2 = speedStats.m2,
                    courseCount = courseStats.count,
                    courseMean = courseStats.mean,
                    courseM2 = courseStats.m2,
                    lastLocationId = lastLocationId,
                    lastLatitude = lastLatitude,
                    lastLongitude = lastLongitude,
                    lastLocationTimestamp = lastLocationTimestamp
                )
            }

            tripFeatureStateDao.upsert(state.copy(sync = false))
        }
    }

    private data class RunningStats(
        val count: Int,
        val mean: Double,
        val m2: Double
    )

    private fun updateRunningStats(stats: RunningStats, value: Double): RunningStats {
        val newCount = stats.count + 1
        val delta = value - stats.mean
        val newMean = stats.mean + delta / newCount
        val delta2 = value - newMean
        val newM2 = stats.m2 + delta * delta2
        return RunningStats(newCount, newMean, newM2)
    }

    private fun updateMean(count: Int, mean: Double, value: Double): Pair<Int, Double> {
        val newCount = count + 1
        val newMean = mean + (value - mean) / newCount
        return newCount to newMean
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
        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = atan2(x, y)
        bearing = Math.toDegrees(bearing)
        bearing = (bearing + 360) % 360
        return bearing
    }

}
