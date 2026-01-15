package com.uoa.sensor.presentation.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.driverprofile.presentation.ui.navigation.navigateToOnboardingInfoScreen
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SensorControlScreenRoute(
    navController: NavController,
    sensorViewModel: SensorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)

    // If no driver profile is found, navigate to the onboarding (driver profile creation) screen.
    if (profileIdString.isNullOrEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigateToOnboardingInfoScreen()
        }
        return
    }

    // Try to parse the stored profile ID.
    try {
        UUID.fromString(profileIdString)
    } catch (e: Exception) {
        // If parsing fails, clear the stored value and navigate to onboarding.
        prefs.edit().remove(DRIVER_PROFILE_ID).apply()
        LaunchedEffect(Unit) {
            navController.navigateToOnboardingInfoScreen()
        }
        return
    }

    // Proceed with the SensorControlScreenUpdate if a valid driver profile exists.
    SensorControlScreenUpdate(
        navController = navController,
        sensorViewModel = sensorViewModel
    )
}
