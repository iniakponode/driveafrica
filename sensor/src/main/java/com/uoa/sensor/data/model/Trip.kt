package com.uoa.sensor.data.model

import java.util.UUID

data class Trip(
    val id: UUID,
    val driverProfileId: Long?,
    val startTime: Long,
    var endTime: Long?,
    var synced: Boolean=false
)
