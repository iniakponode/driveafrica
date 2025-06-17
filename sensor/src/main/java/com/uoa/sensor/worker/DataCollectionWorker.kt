package com.uoa.sensor.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.UUID

//@HiltWorker
//class DataCollectionWorker @AssistedInject constructor(
//    @Assisted context: Context,
//    @Assisted params: WorkerParameters,
//    private val hardwareModule: HardwareModule
//) : CoroutineWorker(context, params) {
//
//    override suspend fun doWork(): Result {
//        val tripIdString = inputData.getString("TRIP_ID")
//        val tripId = tripIdString?.let { UUID.fromString(it) }
//
//        if (tripId == null) {
//            Log.e("DataCollectionWorker", "Trip ID is null")
//            return Result.failure()
//        }
//
//        try {
//            // Initiate Data Collection and Hybrid Motion Detection
//
//            hardwareModule.startDataCollection(tripId )
//            // Notify once when vehicle starts moving
//                    notificationManager.displayNotification("Vehicle State","Sensor data collection has started.")
//            Log.d("DataCollectionWorker", "Started hybrid motion detection for trip: $tripId")
//
//            // Keep the worker running until explicitly stopped or cancelled
//            // Note: Replace with your condition to stop the collection if required
//            while (!isStopped) {
//                // Maintain the loop while data collection is ongoing
//                delay(5000)
//            }
//
//            // Stop Data Collection
//            hardwareModule.stopDataCollection()
//            Log.d("DataCollectionWorker", "Data collection stopped for trip: $tripId")
//        } catch (e: Exception) {
//            Log.e("DataCollectionWorker", "Error during data collection", e)
//            return Result.failure()
//        }
//
//        return Result.success()
//    }
//
//    override fun onStopped() {
//        super.onStopped()
//        // Ensure proper cleanup
//        hardwareModule.stopDataCollection()
//        Log.d("DataCollectionWorker", "Data collection worker stopped and cleaned up")
//    }
//}
//
