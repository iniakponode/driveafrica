package com.uoa.core

import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.DatePickerDefaults.dateFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.Trip
import com.uoa.core.model.SyncState
import java.text.SimpleDateFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

class Converters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            LocalDate.parse(it, formatter)
        }
    }

    @TypeConverter
    fun fromTimestamp(value: Instant?): Long? {
        return value?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun fromDate(date: Date?): String? {
        return date?.let { dateFormat.format(it) }
    }

    @TypeConverter
    fun toDate(dateString: String?): Date? {
        return try {
            dateString?.let {
                dateFormat.parse(it)
            }
        } catch (e: Exception) {
            Log.e("Converters", "Error parsing date: ${e.message}. Input was: $dateString", e)
            // Fallback mechanism: Handle error gracefully, such as returning null or a default date
            null // Return null or a specific default date (e.g., Date(0) or Date())
        }
    }


    @TypeConverter
    fun fromFloatList(value: String?): List<Float>? {
        if (value == null) return null
        if (value.isBlank()) return emptyList()
        val listType = object : TypeToken<List<Float>>() {}.type
        return try {
            val json = if (value.startsWith(FLOAT_LIST_PREFIX)) {
                val payload = value.removePrefix(FLOAT_LIST_PREFIX)
                val compressed = Base64.decode(payload, Base64.NO_WRAP)
                decompressToString(compressed)
            } else {
                value
            }
            Gson().fromJson(json, listType)
        } catch (e: Exception) {
            Log.e("Converters", "Failed to parse float list, falling back to empty list.", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromFloatList(list: List<Float>?): String? {
        if (list == null) return null
        val json = Gson().toJson(list)
        return try {
            val compressed = compressToBytes(json)
            FLOAT_LIST_PREFIX + Base64.encodeToString(compressed, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("Converters", "Failed to compress float list; storing as JSON string.", e)
            json
        }
    }

    private val gson = Gson()
    private val FLOAT_LIST_PREFIX = "gz:"

//    // Convert LocalDate to String and back
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
//    @TypeConverter
//    fun fromLocalDate(value: LocalDate?): String? {
//        return value?.format(dateFormatter)
//    }
//
//    @TypeConverter
//    fun toLocalDate(value: String?): LocalDate? {
//        return value?.let { LocalDate.parse(it, dateFormatter) }
//    }

    // Convert LocalDateTime to String and back
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }

    // Convert Duration to String and back
    // Using the standard ISO-8601 duration format (e.g. PT1H2M3S)
    @TypeConverter
    fun fromDuration(duration: java.time.Duration?): String? {
        return duration?.toString()
    }

    @TypeConverter
    fun toDuration(durationString: String?): java.time.Duration? {
        return durationString?.let { java.time.Duration.parse(it) }
    }

    @TypeConverter
    fun fromSyncState(state: SyncState?): String? {
        return state?.name
    }

    @TypeConverter
    fun toSyncState(value: String?): SyncState? {
        return value?.let { SyncState.valueOf(it) }
    }

    // Convert complex objects stored as JSON strings
    // Map<UUID, Int>
    @TypeConverter
    fun fromMapUUIDInt(map: Map<UUID, Int>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toMapUUIDInt(json: String): Map<UUID, Int> {
        val type = object : TypeToken<Map<UUID, Int>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    // Map<LocalDate, Int>
    @OptIn(ExperimentalMaterial3Api::class)
    @TypeConverter
    fun fromMapLocalDateInt(map: Map<LocalDate, Int>): String {
        // Convert keys to Strings first since LocalDate is not a primitive
        val stringKeyMap = map.mapKeys { it.key.format(dateFormatter) }
        return gson.toJson(stringKeyMap)
    }

    @TypeConverter
    fun toMapLocalDateInt(json: String): Map<LocalDate, Int> {
        val stringType = object : TypeToken<Map<String, Int>>() {}.type
        val stringKeyMap: Map<String, Int> = gson.fromJson(json, stringType) ?: emptyMap()
        return stringKeyMap.mapKeys { LocalDate.parse(it.key, dateFormatter) }
    }

    // Lists and other complex structures (e.g., BehaviourOccurrence)
    // Assuming BehaviourOccurrence is a data class that can be handled by Gson directly
    @TypeConverter
    fun fromBehaviourOccurrenceList(list: List<BehaviourOccurrence>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toBehaviourOccurrenceList(json: String): List<BehaviourOccurrence> {
        val type = object : TypeToken<List<BehaviourOccurrence>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // For Trip object stored as JSON
    @TypeConverter
    fun fromTrip(trip: Trip?): String? {
        return trip?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toTrip(json: String?): Trip? {
        return json?.let {
            gson.fromJson(json, Trip::class.java)
        }
    }

    private fun compressToBytes(payload: String): ByteArray {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).use { gzip ->
            gzip.write(payload.toByteArray(Charsets.UTF_8))
        }
        return byteStream.toByteArray()
    }

    private fun decompressToString(payload: ByteArray): String {
        return try {
            GZIPInputStream(ByteArrayInputStream(payload)).use { gzip ->
                gzip.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            Log.e("Converters", "Failed to decompress payload.", e)
            ""
        }
    }

    // For other maps that use LocalDate as key, just re-use the fromMapLocalDateInt and toMapLocalDateInt
    // and create similar converters if the value type is different.

    // Example: If tripsPerAggregationUnit is Map<LocalDate, Int>, we already have that.
    // If you have different structures (e.g., Map<LocalDate, AnotherObject>),
    // create similar converters, converting keys to strings and values to JSON.

}
