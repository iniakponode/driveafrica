package com.uoa.core.utils

fun buildSpeedLimitQuery(lat: Double, lon: Double, radius: Double): String {
    return """
        [out:json];
        way(around:$radius,$lat,$lon)[highway][maxspeed];
        out tags center;
    """.trimIndent()
}