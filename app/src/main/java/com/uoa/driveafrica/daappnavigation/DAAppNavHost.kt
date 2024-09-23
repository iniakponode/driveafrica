package com.uoa.driveafrica.daappnavigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.uoa.driveafrica.DAAppState
import com.uoa.driverprofile.presentation.ui.navigation.ONBOARDING_SCREEN_ROUTE
import com.uoa.driverprofile.presentation.ui.navigation.drivingTipDetailsScreen
import com.uoa.driverprofile.presentation.ui.navigation.homeScreen

import com.uoa.driverprofile.presentation.ui.navigation.onboardingScreen
import com.uoa.nlgengine.presentation.ui.reportNavigation.filterScreen
import com.uoa.nlgengine.presentation.ui.reportNavigation.reportScreen
import com.uoa.sensor.presentation.ui.sensornavigation.sensorControlScreen


@Composable
fun DAAppNavHost(
    appState: DAAppState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    startDestination: String = ONBOARDING_SCREEN_ROUTE
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        onboardingScreen(
            navController = navController,
            onShowSnackbar = onShowSnackbar
        )
        homeScreen(navController)
        drivingTipDetailsScreen(navController)
        reportScreen(navController)
        filterScreen(navController)
        sensorControlScreen()
    }
}