package com.uoa.safedriveafrica

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
import com.uoa.core.apiServices.workManager.scheduleDataUploadWork
import com.uoa.driverprofile.worker.DailyDrivingTipWorker
import com.uoa.core.utils.SecureTokenStorage
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var secureTokenStorage: SecureTokenStorage
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        secureTokenStorage.getToken()?.takeIf { it.isNotBlank() }?.let {
            scheduleDataUploadWork(this)
        }

        val deleteWorkRequest = PeriodicWorkRequestBuilder<DeleteLocalDataWorker>(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
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

        val dailyTipsWorkRequest = PeriodicWorkRequestBuilder<DailyDrivingTipWorker>(
            1,
            TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "DailyDrivingTips",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyTipsWorkRequest
            )


    }

}
