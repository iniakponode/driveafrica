package com.uoa.sensor.presentation.ui.sensornavigation

// write the the code to define the sensor route, navigation builder screen and route screen
// import SensorControlScreen
// import SensorDataCollectionScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.sensor.presentation.ui.SensorControlScreen
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.uoa.sensor.presentation.ui.SensorControlScreenRoute

const val SENSOR_CONTROL_SCREEN_ROUTE = "sensorControlScreen"
fun NavGraphBuilder.sensorControlScreen() {
    composable(route = SENSOR_CONTROL_SCREEN_ROUTE) {
        SensorControlScreenRoute(
            sensorViewModel = hiltViewModel<SensorViewModel>(),
            tripViewModel = hiltViewModel<TripViewModel>()
        )
    }
}

