package com.uoa.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

object DateUtils {
    fun convertToLocalDate(epochMilli: Long): LocalDate {
        val zoneId = ZoneId.systemDefault()
        return Instant.ofEpochMilli(epochMilli).atZone(zoneId).toLocalDate()
    }

    fun convertToEpochMilli(localDate: LocalDate): Long {
        val zoneId = ZoneId.systemDefault()
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    fun convertLocalDateToDate(localDate: LocalDate): Date {
        val zoneId = ZoneId.systemDefault()
        val instant = localDate.atStartOfDay(zoneId).toInstant()
        return Date.from(instant)
    }

}