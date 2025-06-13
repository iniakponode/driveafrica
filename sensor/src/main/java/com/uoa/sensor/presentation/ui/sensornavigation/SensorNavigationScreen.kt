package com.uoa.sensor.presentation.ui.sensornavigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.uoa.sensor.presentation.ui.SensorControlScreenRoute
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel

const val SENSOR_CONTROL_SCREEN_ROUTE = "sensorControlScreen"

@RequiresApi(Build.VERSION_CODES.Q)
fun NavGraphBuilder.sensorControlScreen(navController: NavController) {
    composable(route = SENSOR_CONTROL_SCREEN_ROUTE) {
        SensorControlScreenRoute(
            navController = navController,
            sensorViewModel = hiltViewModel<SensorViewModel>(),
            tripViewModel = hiltViewModel<TripViewModel>()
        )
    }
}

