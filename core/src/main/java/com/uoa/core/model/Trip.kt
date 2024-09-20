package com.uoa.core.model

import java.sql.Timestamp
import java.util.Date
import java.util.UUID

data class Trip(
    val id: UUID,
    val driverPId: UUID?,
    val startTime: Long,
    var endTime: Long?,
    val startDate: Date?,
    var endDate: Date?,
    var synced: Boolean=false
)
