package com.uoa.sensor.data.model

import java.util.UUID

/**
 * Optional class for location data if needed to pair with sensor data.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param altitude The altitude of the location (if available).
 * @param speed The speed at the location (if available).
 */
data class LocationData(
    val id: UUID,
    val latitude: Long,
    val longitude: Long,
    val altitude: Double? = null,
    val speed: Double? = null,
    val distance: Double? = null,
    val timestamp: Long,
    val sync: Boolean=false
)