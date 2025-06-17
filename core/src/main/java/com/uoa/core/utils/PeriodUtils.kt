package com.uoa.core.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Date

object PeriodUtils {

    fun getReportingPeriod(periodType: PeriodType): Pair<LocalDate, LocalDate>? {
        val zoneId = ZoneId.systemDefault()
        val today = ZonedDateTime.now(zoneId).toLocalDate()

        return when (periodType) {
            PeriodType.TODAY -> {
                Pair(today, today)
            }
            PeriodType.THIS_WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(startOfWeek, endOfWeek)
            }
            PeriodType.LAST_WEEK -> {
                val lastWeek = today.minusWeeks(1)
                val startOfLastWeek = lastWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfLastWeek = lastWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(startOfLastWeek, endOfLastWeek)
            }
            else -> null
        }
    }

    fun LocalDate.toJavaUtilDate(): Date {
        return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
}