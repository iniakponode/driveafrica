package com.uoa.core.nlg

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

// Step 1: Read JSON data from the file
suspend fun readJsonData(context: Context): List<Map<String, String>> = withContext(Dispatchers.IO) {
    try {
        // Read the JSON file from assets or raw resources (change path if necessary)
        val inputStream = context.assets.open("national_traffic_offences_laws_and_penalties.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        // Parse the JSON array
        val jsonArray = JSONArray(jsonString)
        val listOfOffences = mutableListOf<Map<String, String>>()

        // Iterate over each object and convert to a Map
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val offenceMap = mutableMapOf<String, String>()

            // Extract each key-value pair from the JSON object
            offenceMap["offence"] = jsonObject.getString("offence")
            offenceMap["meaning"] = jsonObject.getString("meaning")
            offenceMap["penalty"] = jsonObject.getString("penalty")
            offenceMap["fine"] = jsonObject.getString("fine")
            offenceMap["law"] = jsonObject.getString("law")
            offenceMap["source"] = jsonObject.getString("source")

            // Add the map to the list
            listOfOffences.add(offenceMap)
        }
        listOfOffences
    } catch (e: IOException) {
        Log.e("Error", "Failed to read JSON data", e)
        emptyList()
    }
}

// Step 2: Compress the JSON data
fun compressJsonData(jsonData: List<Map<String, String>>): ByteArray {
    val jsonString = jsonData.joinToString(separator = "\n") { it.toString() }

    // Use Deflater for compression
    val byteArrayOutputStream = ByteArrayOutputStream()
    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    val deflaterOutputStream = DeflaterOutputStream(byteArrayOutputStream, deflater)

    deflaterOutputStream.write(jsonString.toByteArray())
    deflaterOutputStream.close()

    return byteArrayOutputStream.toByteArray()
}

// Step 3: Encode the compressed data to Base64
fun encodeToBase64(data: ByteArray): String {
    return Base64.encodeToString(data, Base64.DEFAULT)
}

// Step 4: Utility function to compress and encode the JSON data
suspend fun compressAndEncodeJson(context: Context): String {
    // Step 1: Read the JSON data
    val jsonData = readJsonData(context)

    // Step 2: Compress the data
    val compressedData = compressJsonData(jsonData)

    // Step 3: Encode the compressed data to Base64
    return encodeToBase64(compressedData)
}

fun getRelevantDataFromJson(context:Context, unsafeBehaviorType: String): String? {

    // Read the JSON file from assets or raw resources (change path if necessary)
    val inputStream = context.assets.open("national_traffic_offences_laws_and_penalties.json")
    val jsonString = inputStream.bufferedReader().use { it.readText() }
    // Parse the JSON string into a JSONArray
    val jsonArray = JSONArray(jsonString)

    // Iterate through the JSON array and find the matching offense based on the unsafe behavior type
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val offense = jsonObject.getString("offence")

        // If the unsafe behavior type matches the offense, return the relevant JSON data
        if (offense.contains(unsafeBehaviorType, ignoreCase = true)) {
            Log.d("JsonMatching", "Found relevant data for behavior: $unsafeBehaviorType")
            return jsonObject.toString() // Return the matching JSON object as a string
        }
    }

    Log.d("JsonMatching", "No relevant data found for behavior: $unsafeBehaviorType")
    return null // Return null if no relevant data is found
}