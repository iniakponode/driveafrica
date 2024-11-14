package com.uoa.core.mlclassifier

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import com.uoa.core.R
import com.uoa.core.mlclassifier.data.MinMax
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinMaxValuesLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val minMaxMap: Map<String, MinMax>

    init {
        minMaxMap = loadMinMaxValues()
    }

    /**
     * Retrieves the minimum value for a given feature.
     *
     * @param featureName The name of the feature.
     * @return The minimum value or null if the feature is not found.
     */
    fun getMin(featureName: String): Float? {
        return minMaxMap[featureName]?.min
    }

    /**
     * Retrieves the maximum value for a given feature.
     *
     * @param featureName The name of the feature.
     * @return The maximum value or null if the feature is not found.
     */
    fun getMax(featureName: String): Float? {
        return minMaxMap[featureName]?.max
    }

    /**
     * Loads and parses the min-max values from the JSON resource.
     *
     * @return An immutable map of feature names to their corresponding MinMax values.
     */
    private fun loadMinMaxValues(): Map<String, MinMax> {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.min_max_values)
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val gson = Gson()
            val minMaxValues = gson.fromJson(jsonString, MinMaxValues::class.java)

            // Validate that all lists have the same size
            if (minMaxValues.featureNames.size != minMaxValues.dataMin.size ||
                minMaxValues.featureNames.size != minMaxValues.dataMax.size
            ) {
                Log.e(
                    "MinMaxValuesLoader",
                    "Mismatch in sizes: featureNames (${minMaxValues.featureNames.size}), " +
                            "dataMin (${minMaxValues.dataMin.size}), dataMax (${minMaxValues.dataMax.size})"
                )
                emptyMap()
            } else {
                // Use zip to pair featureNames with their min and max values
                minMaxValues.featureNames.zip(
                    minMaxValues.dataMin.zip(minMaxValues.dataMax)
                ).associate { (featureName, minMaxPair) ->
                    featureName to MinMax(
                        min = minMaxPair.first.toFloat(),
                        max = minMaxPair.second.toFloat()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MinMaxValuesLoader", "Failed to load min-max values: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * Data class representing the structure of the JSON file.
     */
    private data class MinMaxValues(
        @SerializedName("feature_names") val featureNames: List<String>,
        @SerializedName("data_min") val dataMin: List<Double>,
        @SerializedName("data_max") val dataMax: List<Double>
    )
}