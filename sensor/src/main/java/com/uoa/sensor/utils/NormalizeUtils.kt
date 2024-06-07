package com.uoa.sensor.utils
import java.lang.Math.pow
import kotlin.math.*


object NormalizeUtils {

    fun minMaxNormalize(data: List<Float>): List<Float> {
        val min = data.minOrNull() ?: return data
        val max = data.maxOrNull() ?: return data
        return data.map { (it - min) / (max - min) }
    }

    fun zScoreNormalize(data: List<Float>): List<Float> {
        val mean = data.average().toFloat()
        val stdDev = sqrt(data.map { (it.toDouble() - mean.toDouble().pow(2)) }.average()).toFloat()
        return data.map { (it - mean) / stdDev }
    }
}
