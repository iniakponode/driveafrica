package com.uoa.sensor.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TripTimer {
    var tripStartTime = 0L

    private val _tripDuration = MutableStateFlow("00:00:00")
    val tripDuration = _tripDuration.asStateFlow()

    fun start()
    {
        tripStartTime = System.currentTimeMillis()
    }

    fun stop()
    {
        tripStartTime = 0L
    }

    fun updateDuration(duration: String)
    {
        _tripDuration.value = duration
    }
}