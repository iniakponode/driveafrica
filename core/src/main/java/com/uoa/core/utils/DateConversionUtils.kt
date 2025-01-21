package com.uoa.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateConversionUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd" // Define your desired date format

    fun dateToString(date: Date?): String? {
        return date?.let {
            SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(it)
        }
    }

    fun stringToDate(dateString: String?): Date? {
        return dateString?.let {
            try {
                SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(it)
            } catch (e: Exception) {
                null // Return null if the string cannot be parsed
            }
        }
    }

    fun longToTimestampString(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    fun timestampStringToLong(timestampString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(timestampString) ?: throw IllegalArgumentException("Invalid timestamp string")
        return date.time
    }

}
