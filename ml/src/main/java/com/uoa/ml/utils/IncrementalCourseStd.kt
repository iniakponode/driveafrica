package com.uoa.ml.utils

// IncrementalCourseStd.kt

import com.uoa.core.model.LocationData
import java.util.UUID
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class IncrementalCourseStd @Inject constructor() {
    private var count = 0
    private var mean = 0.0
    private var m2 = 0.0

    private var previousLat: Double? = null
    private var previousLon: Double? = null
    private var previousTimestamp: Long? = null
    private var previousLocationId: UUID? = null

    fun addLocation(location: LocationData) {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) {
            return
        }

        val prevLat = previousLat
        val prevLon = previousLon
        val prevTimestamp = previousTimestamp
        val prevLocationId = previousLocationId

        if (prevLat == null || prevLon == null) {
            previousLat = location.latitude
            previousLon = location.longitude
            previousTimestamp = location.timestamp
            previousLocationId = location.id
            return
        }
        if (location.id == prevLocationId) {
            return
        }
        if (prevTimestamp != null && location.timestamp <= prevTimestamp) {
            return
        }

        val course = calculateBearing(prevLat, prevLon, location.latitude, location.longitude)
        if (!course.isFinite()) {
            return
        }

        count++
        val delta = course - mean
        mean += delta / count
        val delta2 = course - mean
        m2 += delta * delta2

        previousLat = location.latitude
        previousLon = location.longitude
        previousTimestamp = location.timestamp
        previousLocationId = location.id
    }

    fun getStd(): Float {
        if (count < 2) {
            return 0.0f
        }
        val variance = m2 / (count - 1)
        return sqrt(variance).toFloat()
    }

    fun reset() {
        count = 0
        mean = 0.0
        m2 = 0.0
        previousLat = null
        previousLon = null
        previousTimestamp = null
        previousLocationId = null
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
