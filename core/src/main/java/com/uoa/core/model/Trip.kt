package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class Trip(
    val id: UUID,
    val driverPId: UUID?,
    val startTime: Long,
    var endTime: Long?,
    val startDate: Date?,
    var endDate: Date?,
    var influence: String?,
    var sync: Boolean=false,
    val alcoholProbability: Float? = null,
    val userAlcoholResponse: String? = null,
    val syncState: SyncState? = null
)
