package com.uoa.sensor.hardware

import android.content.Context
import android.hardware.Sensor
import com.uoa.sensor.hardware.base.AndroidSensor
import javax.inject.Inject


class LinearAcceleration @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_LINEAR_ACCELERATION)

class AccelerometerSensor @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_ACCELEROMETER)


class GyroscopeSensor @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_GYROSCOPE)

class RotationVectorSensor @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_ROTATION_VECTOR)
class MagnetometerSensor @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_MAGNETIC_FIELD)

class SignificantMotion @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_SIGNIFICANT_MOTION)

class GravitySensor @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_GRAVITY)

class BarometerSensor @Inject constructor(
    context: Context
): AndroidSensor(context=context,
    sensorType=Sensor.TYPE_PRESSURE)