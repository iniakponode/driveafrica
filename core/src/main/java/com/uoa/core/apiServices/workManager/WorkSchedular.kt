package com.uoa.core.apiServices.workManager

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.uoa.core.utils.PreferenceUtils
import java.util.concurrent.TimeUnit

fun scheduleDataUploadWork(context: Context) {
    val allowMetered = PreferenceUtils.isMeteredUploadsAllowed(context)
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED
        )
        .setRequiresCharging(true)
        .build()

    val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadAllDataWorker>(5, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "UploadAllDataWork",
        ExistingPeriodicWorkPolicy.KEEP,
        uploadWorkRequest
    )

    // Observe Work Status
    WorkManager.getInstance(context).getWorkInfoByIdLiveData(uploadWorkRequest.id).observeForever { workInfo ->
        if (workInfo != null) {
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> Log.d("WorkManagerStatus", "Worker is enqueued")
                WorkInfo.State.RUNNING -> Log.d("WorkManagerStatus", "Worker is running")
                WorkInfo.State.SUCCEEDED -> Log.d("WorkManagerStatus", "Worker succeeded")
                WorkInfo.State.FAILED -> Log.d("WorkManagerStatus", "Worker failed")
                WorkInfo.State.CANCELLED -> Log.d("WorkManagerStatus", "Worker cancelled")
                else -> Log.d("WorkManagerStatus", "Worker state: ${workInfo.state}")
            }
        }
    }
}
