package com.uoa.core.model

data class DbdaResult(
    val id: Int,
    val userId: String,
    val tripDataId: Long,
    val harshAcceleration: Boolean,
    val harshDeceleration: Boolean,
    val tailgaiting: Boolean,
    val speeding: Boolean,
    val causes: String,
    val causeUpdated: Boolean,
    val synced: Boolean,
    val timestamp: Long,
    val startDate: String,
    val endDate: String,
    val duration: String,
    val distance: String
    //... other behavior fields
)

