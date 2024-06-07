package com.uoa.driveafrica
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.uoa.driveafrica.ui.components.nav.AppNavigation
//import com.uoa.sensor.presentation.ui.SensorActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
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