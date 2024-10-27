package com.uoa.ml

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorManager
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.RawSensorData
import java.sql.Timestamp
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.pow
import kotlin.math.sqrt

class Utils {

    fun extractHourOfDayMean(timestamps: List<Timestamp>): Float {
        if (timestamps.isEmpty()) return 0.0f

        val calendar = Calendar.getInstance()
        val deviceTimeZone = TimeZone.getDefault()
        var sumHours = 0

        for (timestamp in timestamps) {
            calendar.time = timestamp
            calendar.timeZone = deviceTimeZone
            sumHours += calendar.get(Calendar.HOUR_OF_DAY)
        }

        return sumHours.toFloat() / timestamps.size
    }

    fun extractDayOfWeekMean(timestamps: List<Timestamp>): Float {
        if (timestamps.isEmpty()) return 0.0f

        val calendar = Calendar.getInstance()
        val deviceTimeZone = TimeZone.getDefault()
        var sumDays = 0

        for (timestamp in timestamps) {
            calendar.time = timestamp
            calendar.timeZone = deviceTimeZone
            sumDays += calendar.get(Calendar.DAY_OF_WEEK)
        }

        return sumDays.toFloat() / timestamps.size
    }


    fun extractSpeedStd(speed: List<Float>): Float {
        if (speed.isEmpty()) return 0.0f

        // Calculate the mean of the speed values
        val mean = speed.sum() / speed.size

        // Calculate the variance
        val variance = speed.map { (it - mean) * (it - mean) }.sum() / speed.size

        // Calculate the standard deviation
        return sqrt(variance)
    }

    fun extractAccelerationYOriginalMean(accelerationY: List<Float>): Float {
        if (accelerationY.isEmpty()) return 0.0f

        // Calculate the mean of the accelerationY values
        val mean = accelerationY.sum() / accelerationY.size

        return mean
    }


    // Function to compute the standard deviation of the courses
    fun computeStandardDeviationOfCourses(
        sensorDataList: List<RawSensorDataEntity>,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Float {
        // Step 1: Compute the course for each sensor data item
        val courses = mutableListOf<Float>()

        sensorDataList.forEach { sensorData ->
            val course = computeCourse(sensorData, latitude, longitude, altitude)
            courses.add(course)
        }

        // Step 2: Compute the mean of the courses
        val meanCourse = courses.average().toFloat()

        // Step 3: Compute the standard deviation
        val variance = courses.map { (it - meanCourse).pow(2) }.average().toFloat()
        val standardDeviation = sqrt(variance)

        return standardDeviation
    }

    // Helper function to compute the course for a single sensor data item
    private fun computeCourse(
        sensorData: RawSensorDataEntity,
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Float {
        // Step 1: Initialize required variables
        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)
        var trueHeading = 0.0f
        var deltaGyroHeading = 0.0f

        // Step 2: Extract the rotation vector data
        if (sensorData.sensorType == Sensor.TYPE_ROTATION_VECTOR && sensorData.values.size >= 4) {
            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                floatArrayOf(
                    sensorData.values[0],
                    sensorData.values[1],
                    sensorData.values[2],
                    sensorData.values[3]
                )
            )

            // Step 3: Get the azimuth (course) from the orientation matrix
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            val azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()

            // Step 4: Compensate for magnetic declination using provided latitude, longitude, and altitude
            val declination = getMagneticDeclination(latitude, longitude, altitude)
            trueHeading = (azimuth + declination) % 360
        }

        // Step 5: Extract the gyroscope data to compute heading change
        if (sensorData.sensorType == Sensor.TYPE_GYROSCOPE && sensorData.values.size >= 3) {
            // Assuming the Z-axis represents the rotation around the device's vertical axis
            deltaGyroHeading = sensorData.values[2] * 0.1f // deltaTime is assumed to be 0.1s; adjust as needed
        }

        // Step 6: Apply complementary filter to smooth the course calculation
        val filteredHeading = 0.98f * (trueHeading + deltaGyroHeading) + 0.02f * trueHeading

        return filteredHeading
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