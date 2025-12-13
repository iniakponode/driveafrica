package com.uoa.safedriveafrica
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.apiServices.workManager.UploadAllDataWorker
import com.uoa.core.network.NetworkMonitor
import com.uoa.sensor.hardware.HardwareModule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var hardwareModule: HardwareModule

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // START MOVEMENT DETECTION IMMEDIATELY ON APP LAUNCH
        lifecycleScope.launch {
            android.util.Log.d("MainActivity", "Starting movement detection on app launch")
            hardwareModule.startMovementDetection()
        }

        // Schedule periodic work here
        setupPeriodicUploadWork()

        setContent {
//            AppNavigation()
            val dAppState = rememberDAAppState(networkMonitor)
            DAApp(dAppState)
        }
    }

    private fun setupPeriodicUploadWork() {
        val allowMetered = PreferenceUtils.isMeteredUploadsAllowed(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED
            )
            .setRequiresCharging(true)
            .build()

        val uploadRequest = PeriodicWorkRequestBuilder<UploadAllDataWorker>(
            15, TimeUnit.MINUTES // This might still be 15 min on older devices.
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "UploadRawData",
                ExistingPeriodicWorkPolicy.KEEP,
                uploadRequest
            )
    }
}

//@Composable
//fun MainScreen() {
//    val context = LocalContext.current
//    Button(onClick = {
//        val intent = Intent(context, SensorActivity::class.java)
//        context.startActivity(intent)
//    }) {
//        Text("Go to Sensor Control")
//    }
//}