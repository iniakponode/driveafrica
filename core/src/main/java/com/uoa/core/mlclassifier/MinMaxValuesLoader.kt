package com.uoa.core.mlclassifier

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import com.uoa.core.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
class MinMaxValuesLoader(@ApplicationContext private val context: Context) {

    private val minMaxMap: Map<String, Pair<Float, Float>>

    init {
        val inputStream: InputStream = context.resources.openRawResource(R.raw.min_max_values)
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val gson = Gson()
        val minMaxValues = gson.fromJson(jsonString, MinMaxValues::class.java)

        val tempMap = mutableMapOf<String, Pair<Float, Float>>()
        for (i in minMaxValues.featureNames.indices) {
            val featureName = minMaxValues.featureNames[i]
            val minValue = minMaxValues.dataMin[i].toFloat()
            val maxValue = minMaxValues.dataMax[i].toFloat()
            tempMap[featureName] = Pair(minValue, maxValue)
        }
        minMaxMap = tempMap
    }

    fun getMin(featureName: String): Float? {
        return minMaxMap[featureName]?.first
    }

    fun getMax(featureName: String): Float? {
        return minMaxMap[featureName]?.second
    }

    private data class MinMaxValues(
        @SerializedName("feature_names") val featureNames: List<String>,
        @SerializedName("data_min") val dataMin: List<Double>,
        @SerializedName("data_max") val dataMax: List<Double>
    )
}