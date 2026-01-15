package com.uoa.core.apiServices.workManager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun enqueueImmediateUploadWork(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadAllDataWorker>()
        .setConstraints(constraints)
        .addTag("UploadAllDataNow")
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "UploadAllDataNow",
        ExistingWorkPolicy.REPLACE,
        uploadWorkRequest
    )
}

fun scheduleDataUploadWork(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadAllDataWorker>(5, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "UploadAllDataWork",
        ExistingPeriodicWorkPolicy.KEEP,
        uploadWorkRequest
    )
}