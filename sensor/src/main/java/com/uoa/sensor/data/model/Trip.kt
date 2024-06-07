package com.uoa.sensor.data.model

data class Trip(
    val id: Long = 0,
    val driverProfileId: Long?,
    val startTime: Long,
    var endTime: Long?,
    var synced: Boolean=false
)
