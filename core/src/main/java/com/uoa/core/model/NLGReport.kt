package com.uoa.sensor.data.model

data class NLGReport(
    val id: Int,
    val userId: String,
    val reportText: String,
    val dateRange: String,
    val synced: Boolean,
)
