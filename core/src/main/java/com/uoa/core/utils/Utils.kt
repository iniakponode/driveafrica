package com.uoa.core.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun buildSpeedLimitQuery(lat: Double, lon: Double, radius: Double): String {
    return """
        [out:json];
        way(around:$radius,$lat,$lon)[highway][maxspeed];
        out tags center;
    """.trimIndent()
}


// Convert a LocalDate to an ISO‑8601 string with offset (UTC+1)
fun formatDateToUTCPlusOne(date: LocalDate): String {
    // Start of day for the given LocalDate.
    val localDateTime = date.atStartOfDay()
    // Convert to a ZonedDateTime and adjust to UTC+1.
    val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneId.of("UTC+1"))
    // Use a formatter that includes the offset.
    return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

// Convert LocalDate to java.util.Date:
fun localDateToDate(localDate: LocalDate): Date {
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
}
fun dateToLocalDate(date: Date): LocalDate {
    return date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}


//fun scheduleNextUploadWork(context: Context) {
//    val uploadWork = OneTimeWorkRequestBuilder<UploadAllDataWorker>()
//        .setInitialDelay(3, TimeUnit.MINUTES)
//        .build()
//
//    WorkManager.getInstance(context).
//    enqueue(
//        uploadWork)
//
//    // Observe work completion and schedule next
//    WorkManager.getInstance(context)
//        .getWorkInfoByIdLiveData(uploadWork.id)
//        .observeForever { workInfo ->
//            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
//                // Schedule the next upload after a 3 minute delay
////                scheduleNextUploadWork(context)
//            }
//        }
//}

