//package com.uoa.ml
//
//
//import android.hardware.GeomagneticField
//import android.hardware.Sensor
//import android.hardware.SensorManager
//import com.uoa.core.database.entities.RawSensorDataEntity
//import java.sql.Timestamp
//import java.util.Calendar
//import java.util.TimeZone
//import kotlin.math.pow
//import kotlin.math.sqrt
//
//class Utils {
//
//    // Replace these placeholder min and max values with the actual values from your training data
//    companion object {
//        const val SPEED_STD_MIN = /* your min value */ 0.0f     // Replace with your actual min value
//        const val SPEED_STD_MAX = /* your max value */ 50.0f    // Replace with your actual max value
//
//        const val COURSE_STD_MIN = /* your min value */ 0.0f    // Replace with your actual min value
//        const val COURSE_STD_MAX = /* your max value */ 180.0f  // Replace with your actual max value
//
//        const val ACCEL_Y_MEAN_MIN = /* your min value */ -20.0f // Replace with your actual min value
//        const val ACCEL_Y_MEAN_MAX = /* your max value */ 20.0f  // Replace with your actual max value
//    }
//
//    fun extractNormalizedHourOfDayMean(timestamps: List<Timestamp>, timeZone: TimeZone): Float {
//        if (timestamps.isEmpty()) return 0.0f
//
//        val calendar = Calendar.getInstance(timeZone)
//        val hoursOfDay = mutableListOf<Int>()
//
//        for (timestamp in timestamps) {
//            calendar.timeInMillis = timestamp.time
//            hoursOfDay.add(calendar.get(Calendar.HOUR_OF_DAY))
//        }
//
//        // Normalize the hours of day (0 to 23)
//        val normalizedHours = hoursOfDay.map { it.toFloat() / 23.0f }
//
//        // Calculate the mean of normalized hours
//        val sumNormalizedHours = normalizedHours.sum()
//        return sumNormalizedHours / normalizedHours.size
//    }
//
//    fun extractNormalizedDayOfWeekMean(timestamps: List<Timestamp>, timeZone: TimeZone): Float {
//        if (timestamps.isEmpty()) return 0.0f
//
//        val calendar = Calendar.getInstance(timeZone)
//        val daysOfWeek = mutableListOf<Int>()
//
//        for (timestamp in timestamps) {
//            calendar.timeInMillis = timestamp.time
//            daysOfWeek.add(calendar.get(Calendar.DAY_OF_WEEK))
//        }
//
//        // Normalize the days of week (1 to 7)
//        val normalizedDays = daysOfWeek.map { (it - 1).toFloat() / 6.0f } // Days mapped to 0-6
//
//        // Calculate the mean of normalized days
//        val sumNormalizedDays = normalizedDays.sum()
//        return sumNormalizedDays / normalizedDays.size
//    }
//
//    fun extractNormalizedSpeedStd(speeds: List<Float>): Float {
//        if (speeds.isEmpty()) return 0.0f
//
//        val meanSpeed = speeds.average().toFloat()
//        val variance = speeds.map { (it - meanSpeed).pow(2) }.average().toFloat()
//        val speedStd = sqrt(variance)
//
//        val range = SPEED_STD_MAX - SPEED_STD_MIN
//        val normalizedSpeedStd = if (range != 0f) {
//            (speedStd - SPEED_STD_MIN) / range
//        } else {
//            0.0f // Handle division by zero if min and max are equal
//        }
//        return normalizedSpeedStd.coerceIn(0.0f, 1.0f)
//    }
//
//    fun extractNormalizedAccelerationYOriginalMean(accelerationYValues: List<Float>): Float {
//        if (accelerationYValues.isEmpty()) return 0.0f
//
//        val meanAccelY = accelerationYValues.average().toFloat()
//
//        val range = ACCEL_Y_MEAN_MAX - ACCEL_Y_MEAN_MIN
//        val normalizedMeanAccelY = if (range != 0f) {
//            (meanAccelY - ACCEL_Y_MEAN_MIN) / range
//        } else {
//            0.0f
//        }
//        return normalizedMeanAccelY.coerceIn(0.0f, 1.0f)
//    }
//
//    // Function to compute the normalized standard deviation of the courses
//    fun computeNormalizedStandardDeviationOfCourses(
//        sensorDataList: List<RawSensorDataEntity>,
//        latitude: Double,
//        longitude: Double,
//        altitude: Double
//    ): Float {
//        // Step 1: Compute the course for each sensor data item
//        val courses = mutableListOf<Float>()
//
//        // Variables to keep track of previous heading and timestamp
//        var previousHeading: Float? = null
//        var previousTimestamp: Long? = null
//
//        for (sensorData in sensorDataList) {
//            val (course, timestamp) = computeCourse(sensorData, latitude, longitude, altitude, previousHeading, previousTimestamp)
//            if (!course.isNaN()) {
//                courses.add(course)
//                previousHeading = course
//                previousTimestamp = timestamp
//            }
//        }
//
//        if (courses.isEmpty()) return 0.0f
//
//        // Step 2: Compute the mean of the courses
//        val meanCourse = courses.average().toFloat()
//
//        // Step 3: Compute the standard deviation
//        val variance = courses.map { (it - meanCourse).pow(2) }.average().toFloat()
//        val standardDeviation = sqrt(variance)
//
//        val range = COURSE_STD_MAX - COURSE_STD_MIN
//        val normalizedCourseStd = if (range != 0f) {
//            (standardDeviation - COURSE_STD_MIN) / range
//        } else {
//            0.0f
//        }
//        return normalizedCourseStd.coerceIn(0.0f, 1.0f)
//    }
//
//    // Helper function to compute the course for a single sensor data item
//    private fun computeCourse(
//        sensorData: RawSensorDataEntity,
//        latitude: Double,
//        longitude: Double,
//        altitude: Double,
//        previousHeading: Float?,
//        previousTimestamp: Long?
//    ): Pair<Float, Long> {
//        val rotationMatrix = FloatArray(9)
//        val orientationValues = FloatArray(3)
//        var currentHeading: Float? = null
//
//        if (sensorData.sensorType == Sensor.TYPE_ROTATION_VECTOR && sensorData.values.size >= 4) {
//            // Use the rotation vector to get the rotation matrix
//            SensorManager.getRotationMatrixFromVector(
//                rotationMatrix,
//                sensorData.values.take(4).toFloatArray()
//            )
//
//            // Get the azimuth (rotation around the Z axis)
//            SensorManager.getOrientation(rotationMatrix, orientationValues)
//            var azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
//
//            // Adjust azimuth to be between 0 and 360
//            if (azimuth < 0) {
//                azimuth += 360.0f
//            }
//
//            // Compensate for magnetic declination
//            val declination = getMagneticDeclination(latitude, longitude, altitude)
//            currentHeading = (azimuth + declination) % 360.0f
//
//            return Pair(currentHeading, sensorData.timestamp)
//        }
//
//        if (sensorData.sensorType == Sensor.TYPE_GYROSCOPE && sensorData.values.size >= 3 && previousHeading != null && previousTimestamp != null) {
//            // Calculate time difference in seconds
//            val deltaTime = (sensorData.timestamp - previousTimestamp) / 1_000_000_000.0f
//
//            // Gyroscope values represent angular velocity in radians per second
//            val angularSpeedZ = sensorData.values[2] // rotation around Z-axis
//
//            // Update heading based on angular speed
//            val deltaAngle = Math.toDegrees(angularSpeedZ * deltaTime.toDouble()).toFloat()
//            currentHeading = (previousHeading + deltaAngle) % 360.0f
//
//            if (currentHeading < 0) {
//                currentHeading += 360.0f
//            }
//
//            return Pair(currentHeading, sensorData.timestamp)
//        }
//
//        // If data is not available, return NaN and the previous timestamp
//        return Pair(Float.NaN, sensorData.timestamp)
//    }
//
//    private fun getMagneticDeclination(
//        latitude: Double,
//        longitude: Double,
//        altitude: Double
//    ): Float {
//        val calendar = Calendar.getInstance()
//        val geomagneticField = GeomagneticField(
//            latitude.toFloat(),
//            longitude.toFloat(),
//            altitude.toFloat(),
//            calendar.timeInMillis
//        )
//        return geomagneticField.declination
//    }
//}
