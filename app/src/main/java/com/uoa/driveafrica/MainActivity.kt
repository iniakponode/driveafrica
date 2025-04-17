package com.uoa.driveafrica
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.uoa.core.apiServices.workManager.UploadAllDataWorker
//import com.uoa.core.utils.TimeZoneMonitor
import com.uoa.core.utils.internetconnectivity.NetworkMonitor
//import com.uoa.driveafrica.ui.daappnavigation.AppNavigation
//import com.uoa.sensor.presentation.ui.SensorActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

//    @Inject
//    lateinit var timeZoneMonitor: TimeZoneMonitor

override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    // Schedule periodic work here
        setupPeriodicUploadWork()
    setContent {
//            AppNavigation()
            val dAppState = rememberDAAppState(networkMonitor)
            DAApp(dAppState)
        }
    }

    private fun setupPeriodicUploadWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
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