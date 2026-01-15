package com.uoa.safedriveafrica
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uoa.core.apiServices.session.SessionManager
import com.uoa.core.network.NetworkMonitor
import com.uoa.core.utils.Constants.Companion.EXTRA_NAVIGATE_ROUTE
import com.uoa.safedriveafrica.ui.theme.DriveAfricaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor
    @Inject
    lateinit var sessionManager: SessionManager
    private val pendingRoute = MutableStateFlow<String?>(null)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = intent?.getStringExtra(EXTRA_NAVIGATE_ROUTE)

        setContent {
            DriveAfricaTheme {
                val dAppState = rememberDAAppState(networkMonitor)
                val initialRoute = pendingRoute.collectAsStateWithLifecycle().value
                DAApp(
                    dAppState,
                    sessionManager = sessionManager,
                    initialRoute = initialRoute,
                    onInitialRouteConsumed = { pendingRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_NAVIGATE_ROUTE)
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
