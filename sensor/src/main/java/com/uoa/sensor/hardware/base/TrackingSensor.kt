package com.uoa.sensor.hardware.base

import java.time.Instant

abstract class TrackingSensor(
    protected val sensorType: Int
) {
    protected var onSensorValueChanged: ((List<Float>) -> Unit)? = null
    protected var whenSensorValueChanges: ((Int, List<Float>, Int) -> Unit)? = null  // Includes sensor type and accuracy now
    protected var onAccuracyValueChanged: ((Int) -> Unit)? = null
    protected var sensorTimestamp: ((Instant) -> Unit)? = null

    abstract fun doesSensorExist(): Boolean
    abstract fun startListeningToSensor(rate: Int): Boolean
    abstract fun stopListeningToSensor()

    fun setOnSensorValuesChangedListener(listener: (List<Float>) -> Unit) {
        onSensorValueChanged = listener
    }

    fun whenSensorValueChangesListener(listener: (Int, List<Float>, Int) -> Unit) {
        whenSensorValueChanges = listener
    }

    fun setTimestampOnSensorChangedListener(listener: (Instant) -> Unit) {
        sensorTimestamp = listener
    }

    fun setOnAccuracyValueChangedListener(listener: (Int) -> Unit) {
        onAccuracyValueChanged = listener
    }
}