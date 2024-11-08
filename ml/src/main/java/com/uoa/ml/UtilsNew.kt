package com.uoa.ml

import com.uoa.core.mlclassifier.MinMaxValuesLoader
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorManager
import com.uoa.core.database.entities.RawSensorDataEntity
import java.sql.Timestamp
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

class UtilsNew @Inject constructor(private val minMaxValuesLoader: MinMaxValuesLoader) {

    fun extractNormalizedHourOfDayMean(timestamps: List<Timestamp>, timeZone: TimeZone): Float {
        if (timestamps.isEmpty()) return 0.0f

        val calendar = Calendar.getInstance(timeZone)
        val hoursOfDay = mutableListOf<Float>()

        for (timestamp in timestamps) {
            calendar.timeInMillis = timestamp.time
            val hour = calendar.get(Calendar.HOUR_OF_DAY).toFloat()
            hoursOfDay.add(hour)
        }

        val meanHour = hoursOfDay.average().toFloat()

        val minValue = minMaxValuesLoader.getMin("hour_of_day_mean") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("hour_of_day_mean") ?: 23f

        val range = maxValue - minValue
        val normalizedHourMean = if (range != 0f) {
            (meanHour - minValue) / range
        } else {
            0.0f
        }
        return normalizedHourMean.coerceIn(0.0f, 1.0f)
    }

    fun extractNormalizedDayOfWeekMean(timestamps: List<Timestamp>, timeZone: TimeZone): Float {
        if (timestamps.isEmpty()) return 0.0f

        val calendar = Calendar.getInstance(timeZone)
        val daysOfWeek = mutableListOf<Float>()

        for (timestamp in timestamps) {
            calendar.timeInMillis = timestamp.time
            // Calendar.DAY_OF_WEEK ranges from 1 (Sunday) to 7 (Saturday)
            val day = (calendar.get(Calendar.DAY_OF_WEEK) - 1).toFloat() // Adjust to 0-6
            daysOfWeek.add(day)
        }

        val meanDay = daysOfWeek.average().toFloat()

        val minValue = minMaxValuesLoader.getMin("day_of_week_mean") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("day_of_week_mean") ?: 6f

        val range = maxValue - minValue
        val normalizedDayMean = if (range != 0f) {
            (meanDay - minValue) / range
        } else {
            0.0f
        }
        return normalizedDayMean.coerceIn(0.0f, 1.0f)
    }

    fun extractNormalizedSpeedStd(speeds: List<Float>): Float {
        if (speeds.isEmpty()) return 0.0f

        val meanSpeed = speeds.average().toFloat()
        val variance = speeds.map { (it - meanSpeed).pow(2) }.average().toFloat()
        val speedStd = sqrt(variance)

        val minValue = minMaxValuesLoader.getMin("speed_std") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("speed_std") ?: 100f

        val range = maxValue - minValue
        val normalizedSpeedStd = if (range != 0f) {
            (speedStd - minValue) / range
        } else {
            0.0f
        }
        return normalizedSpeedStd.coerceIn(0.0f, 1.0f)
    }

    fun extractNormalizedAccelerationYOriginalMean(accelerationYValues: List<Float>): Float {
        if (accelerationYValues.isEmpty()) return 0.0f

        val meanAccelY = accelerationYValues.average().toFloat()

        val minValue = minMaxValuesLoader.getMin("accelerationYOriginal_mean") ?: -10f
        val maxValue = minMaxValuesLoader.getMax("accelerationYOriginal_mean") ?: 10f

        val range = maxValue - minValue
        val normalizedMeanAccelY = if (range != 0f) {
            (meanAccelY - minValue) / range
        } else {
            0.0f
        }
        return normalizedMeanAccelY.coerceIn(0.0f, 1.0f)
    }

    // Function to compute the normalized standard deviation of the courses
    fun computeNormalizedStandardDeviationOfCourses(
        sensorDataList: List<RawSensorDataEntity>,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Float {
        // Step 1: Compute the course for each sensor data item
        val courses = mutableListOf<Float>()

        // Variables to keep track of previous heading and timestamp
        var previousHeading: Float? = null
        var previousTimestamp: Long? = null

        for (sensorData in sensorDataList) {
            val (course, timestamp) = computeCourse(
                sensorData,
                latitude,
                longitude,
                altitude,
                previousHeading,
                previousTimestamp
            )
            if (!course.isNaN()) {
                courses.add(course)
                previousHeading = course
                previousTimestamp = timestamp
            }
        }

        if (courses.isEmpty()) return 0.0f

        // Step 2: Compute the mean of the courses
        val meanCourse = courses.average().toFloat()

        // Step 3: Compute the standard deviation
        val variance = courses.map { (it - meanCourse).pow(2) }.average().toFloat()
        val standardDeviation = sqrt(variance)

        val minValue = minMaxValuesLoader.getMin("course_std") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("course_std") ?: 180f

        val range = maxValue - minValue
        val normalizedCourseStd = if (range != 0f) {
            (standardDeviation - minValue) / range
        } else {
            0.0f
        }
        return normalizedCourseStd.coerceIn(0.0f, 1.0f)
    }

    // Helper function to compute the course for a single sensor data item
    private fun computeCourse(
        sensorData: RawSensorDataEntity,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        previousHeading: Float?,
        previousTimestamp: Long?
    ): Pair<Float, Long> {
        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)
        var currentHeading: Float? = null

        if (sensorData.sensorType == Sensor.TYPE_ROTATION_VECTOR && sensorData.values.size >= 4) {
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
            currentHeading = (azimuth + declination) % 360.0f

            return Pair(currentHeading, sensorData.timestamp)
        }

        if (sensorData.sensorType == Sensor.TYPE_GYROSCOPE && sensorData.values.size >= 3 && previousHeading != null && previousTimestamp != null) {
            // Calculate time difference in seconds
            val deltaTime = (sensorData.timestamp - previousTimestamp) / 1_000_000_000.0f

            // Gyroscope values represent angular velocity in radians per second
            val angularSpeedZ = sensorData.values[2] // rotation around Z-axis

            // Update heading based on angular speed
            val deltaAngle = Math.toDegrees(angularSpeedZ * deltaTime.toDouble()).toFloat()
            currentHeading = (previousHeading + deltaAngle) % 360.0f

            if (currentHeading < 0) {
                currentHeading += 360.0f
            }

            return Pair(currentHeading, sensorData.timestamp)
        }

        // If data is not available, return NaN and the previous timestamp
        return Pair(Float.NaN, sensorData.timestamp)
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
}