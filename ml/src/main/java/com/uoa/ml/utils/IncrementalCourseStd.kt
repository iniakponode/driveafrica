package com.uoa.ml.utils

// IncrementalCourseStd.kt

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.math.*

class IncrementalCourseStd @Inject constructor(
    private val minMaxValuesLoader: MinMaxValuesLoader,
    private val locationRepo: LocationRepository
) {
    private var sumSin = 0.0
    private var sumCos = 0.0
    private var count = 0

    // Variables to keep track of previous heading and timestamp
    private var previousHeading: Float? = null
    private var previousTimestamp: Long? = null

    // Cache for location data to avoid redundant database calls
    private val locationCache = mutableMapOf<UUID, LocationEntity?>()

    /**
     * Adds a sensor data point to the incremental calculation of the course standard deviation.
     * This function is suspend because it may perform database operations to retrieve location data.
     *
     * @param sensorData The RawSensorDataEntity containing sensor data.
     */
    suspend fun addSensorData(sensorData: RawSensorDataEntity) {
        val locationId = sensorData.locationId ?: run {
            Log.w("IncrementalCourseStd", "Sensor data has null locationId.")
            return
        }

        val locationData = getLocationData(locationId) ?: run {
            Log.w("IncrementalCourseStd", "No location data found for locationId: $locationId")
            return
        }

        val (latitude, longitude, altitude) = locationData

        val (course, timestamp) = computeCourse(
            sensorData,
            previousHeading,
            previousTimestamp,
            latitude,
            longitude,
            altitude
        )

        if (!course.isNaN()) {
            val angleRadians = Math.toRadians(course.toDouble())
            sumSin += sin(angleRadians)
            sumCos += cos(angleRadians)
            count++
            previousHeading = course
            previousTimestamp = timestamp
            Log.d("IncrementalCourseStd", "Computed Course: $course at Timestamp: $timestamp")
        } else {
            Log.w("IncrementalCourseStd", "Computed NaN course for sensorData: $sensorData")
        }
    }

    /**
     * Computes the normalized standard deviation of the courses added so far.
     *
     * @return The normalized course standard deviation as a Float between 0.0 and 1.0.
     */
    fun getNormalizedStd(): Float {
        if (count == 0) {
            Log.w("IncrementalCourseStd", "No course data provided for standard deviation computation.")
            return 0.0f
        }

        val r = sqrt(sumSin * sumSin + sumCos * sumCos) / count
        // Ensure r is within valid range for ln
        val adjustedR = r.coerceIn(0.0001, 1.0)
        val circularStdDev = sqrt(-2 * ln(adjustedR)).toFloat()

        Log.d("IncrementalCourseStd", "Circular Standard Deviation: $circularStdDev")

        val minValue = minMaxValuesLoader.getMin("course_std") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("course_std") ?: PI.toFloat()
        val range = maxValue - minValue

        val normalizedCourseStd = if (range != 0f) {
            (circularStdDev - minValue) / range
        } else {
            Log.w("IncrementalCourseStd", "Range for course_std is zero. Defaulting normalized value to 0.0f")
            0.0f
        }

        Log.d("IncrementalCourseStd", "Normalized Course Standard Deviation: $normalizedCourseStd")
        return normalizedCourseStd.coerceIn(0.0f, 1.0f)
    }

    // Helper function to compute the course for a single sensor data item
    private fun computeCourse(
        sensorData: RawSensorDataEntity,
        previousHeading: Float?,
        previousTimestamp: Long?,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Pair<Float, Long> {
        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)

        return when {
            sensorData.sensorType == Sensor.TYPE_ROTATION_VECTOR && sensorData.values.size >= 4 -> {
                // Use the rotation vector to get the rotation matrix
                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix,
                    sensorData.values.take(4).toFloatArray()
                )

                // Get the azimuth (rotation around the Z axis)
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                var azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()

                // Adjust azimuth to be between 0 and 360
                if (azimuth < 0) {
                    azimuth += 360.0f
                }

                // Compensate for magnetic declination
                val declination = getMagneticDeclination(latitude, longitude, altitude)
                val currentHeading = (azimuth + declination) % 360.0f

                Pair(currentHeading, sensorData.timestamp)
            }
            sensorData.sensorType == Sensor.TYPE_GYROSCOPE && sensorData.values.size >= 3 &&
                    previousHeading != null && previousTimestamp != null -> {
                // Calculate time difference in seconds
                val deltaTime = (sensorData.timestamp - previousTimestamp) / 1_000_000_000.0f

                // Gyroscope values represent angular velocity in radians per second
                val angularSpeedZ = sensorData.values[2] // rotation around Z-axis

                // Update heading based on angular speed
                val deltaAngle = Math.toDegrees(angularSpeedZ * deltaTime.toDouble()).toFloat()
                var currentHeading = (previousHeading + deltaAngle) % 360.0f

                if (currentHeading < 0) {
                    currentHeading += 360.0f
                }

                Pair(currentHeading, sensorData.timestamp)
            }
            else -> {
                // If data is not available, return NaN and the current timestamp
                Pair(Float.NaN, sensorData.timestamp)
            }
        }
    }

    private fun getMagneticDeclination(
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Float {
        val calendar = Calendar.getInstance()
        val geomagneticField = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude.toFloat(),
            calendar.timeInMillis
        )
        return geomagneticField.declination
    }

    private suspend fun getLocationData(
        locationId: UUID
    ): Triple<Double, Double, Double>? {
        // Check if the location data is already cached
        locationCache[locationId]?.let { locationEntity ->
            return Triple(locationEntity.latitude, locationEntity.longitude, locationEntity.altitude)
        }

        // Fetch location data from the repository
        val locationEntity = withContext(Dispatchers.IO) {
            locationRepo.getLocationById(locationId)
        }

        // Cache the result (even if null to prevent repeated lookups)
        locationCache[locationId] = locationEntity

        return locationEntity?.let {
            Triple(it.latitude, it.longitude, it.altitude)
        }
    }
}