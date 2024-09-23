package com.uoa.driveafrica
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import com.uoa.core.utils.TimeZoneMonitor
import com.uoa.core.utils.internetconnectivity.NetworkMonitor
//import com.uoa.driveafrica.ui.daappnavigation.AppNavigation
//import com.uoa.sensor.presentation.ui.SensorActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

//    @Inject
//    lateinit var timeZoneMonitor: TimeZoneMonitor

override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            AppNavigation()
            val dAppState = rememberDAAppState(networkMonitor)
            DAApp(dAppState)
        }
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