package com.uoa.safedriveafrica.presentation.daappnavigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation.alcoholQuestionnaireScreen
import com.uoa.core.utils.SETTINGS_ROUTE
import com.uoa.core.utils.HELP_ROUTE
import com.uoa.core.utils.SPLASH_ROUTE
import com.uoa.safedriveafrica.DAAppState
import com.uoa.safedriveafrica.ui.splashscreens.splashScreen
import com.uoa.driverprofile.presentation.ui.navigation.drivingTipDetailsScreen
import com.uoa.driverprofile.presentation.ui.navigation.homeScreen
import com.uoa.driverprofile.presentation.ui.navigation.joinFleetScreen

import com.uoa.driverprofile.presentation.ui.navigation.onboardingFormScreen
import com.uoa.driverprofile.presentation.ui.navigation.onboardingInfoScreen
import com.uoa.driverprofile.presentation.ui.navigation.registrationChoiceScreen
import com.uoa.nlgengine.presentation.ui.reportNavigation.filterScreen
import com.uoa.nlgengine.presentation.ui.reportNavigation.reportScreen
import com.uoa.sensor.presentation.ui.sensornavigation.sensorControlScreen
import com.uoa.sensor.presentation.ui.navigation.vehicleDetectionMonitorScreen
import com.uoa.safedriveafrica.presentation.settings.SettingsRoute
import com.uoa.safedriveafrica.presentation.help.HelpRoute
import com.uoa.safedriveafrica.presentation.debug.addDebugRoutes
import androidx.navigation.compose.composable


@Composable
fun DAAppNavHost(
    appState: DAAppState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    startDestination: String = SPLASH_ROUTE
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        splashScreen(navController)
        entryPointScreen(navController)

        onboardingInfoScreen(
            navController = navController,
            onShowSnackbar = onShowSnackbar
        )
        registrationChoiceScreen(navController)
        onboardingFormScreen(
            navController = navController,
            onShowSnackbar = onShowSnackbar
        )
        homeScreen(navController)
        joinFleetScreen(navController)
        drivingTipDetailsScreen(navController)
        reportScreen(navController)
        filterScreen(navController)
        sensorControlScreen(navController)
        composable(SETTINGS_ROUTE) { SettingsRoute(navController = navController) }
        composable(HELP_ROUTE) { HelpRoute() }
        addDebugRoutes(navController)

        // Add the Alcohol Questionnaire screen route
        alcoholQuestionnaireScreen(navController)

        // Vehicle Detection Monitor - Real-time GPS speed and detection monitoring
        vehicleDetectionMonitorScreen()
    }
}
