//package com.uoa.core.network
//
//import android.content.Context
//import com.uoa.core.utils.TimeZoneMonitor
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.datetime.TimeZone
//import kotlinx.datetime.toKotlinTimeZone
//import java.time.ZoneId
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class TimeZoneMonitorImpl @Inject constructor(
//    @ApplicationContext private val context: Context, override val currentTimeZone: Flow<TimeZone>
//) : TimeZoneMonitor {
//
//    val timeZoneFlow: Flow<TimeZone> = callbackFlow {
//        val currentZone = ZoneId.systemDefault().toKotlinTimeZone()
//        trySend(currentZone)
//        // Add logic to listen for timezone changes and emit new values
//        awaitClose { /* Clean up resources if needed */ }
//    }
//}