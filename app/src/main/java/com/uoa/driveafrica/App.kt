package com.uoa.driveafrica

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.uoa.core.apiServices.workManager.DeleteLocalDataWorker
import com.uoa.core.apiServices.workManager.UnsafeDrivingAnalysisWorker
import com.uoa.core.apiServices.workManager.UploadRawSensorDataWorker
//import androidx.work.Constraints
//import androidx.work.ExistingPeriodicWorkPolicy
//import androidx.work.NetworkType
//import androidx.work.PeriodicWorkRequestBuilder
//import androidx.work.WorkManager
//import com.uoa.core.apiServices.workManager.DeleteLocalDataWorker
//import com.uoa.core.apiServices.workManager.UnsafeDrivingAnalysisWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

//        WorkManager.initialize(this, Configuration.Builder()
//            .setMinimumLoggingLevel(android.util.Log.DEBUG)
//            .build())
        // Workers Scheduled here:
//        scheduleDataUploadWork(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadRawSensorDataWorker>(5, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "UploadRawData",
                ExistingPeriodicWorkPolicy.KEEP,
                uploadWorkRequest
            )

        val deleteWorkRequest = PeriodicWorkRequestBuilder<DeleteLocalDataWorker>(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "DeleteLocalData",
                ExistingPeriodicWorkPolicy.KEEP,
                deleteWorkRequest
            )


        val analysisWorkRequest = PeriodicWorkRequestBuilder<UnsafeDrivingAnalysisWorker>(
            3, // repeat interval
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "UnsafeDrivingAnalysisWork",
                ExistingPeriodicWorkPolicy.KEEP, // or REPLACE, depending on your needs
                analysisWorkRequest
            )



    }

}
