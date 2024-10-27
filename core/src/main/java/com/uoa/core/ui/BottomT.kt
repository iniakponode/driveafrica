package com.uoa.core.ui


import kotlinx.serialization.Serializable

@Serializable
data class Home(val driverProfileId: String)

@Serializable
object Reports

@Serializable
object DrivingTips

@Serializable
object RecordTrip

@Serializable
object Analysis

