package com.uoa.core.utils

import com.uoa.core.database.entities.RoadEntity
import kotlin.math.pow
import kotlin.math.round

private fun Double.roundDp(dp: Int): Double {
    val f = 10.0.pow(dp)
    return round(this * f) / f
}

// Include fields that define “the same” road for you:
private fun keyForDedup(r: RoadEntity) = listOf(
    r.driverProfileId, r.name, r.roadType, r.speedLimit, r.radius,
    r.latitude.roundDp(5), r.longitude.roundDp(5)
)
