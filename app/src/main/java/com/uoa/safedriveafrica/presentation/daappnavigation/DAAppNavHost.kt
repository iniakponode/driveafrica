package com.uoa.safedriveafrica.presentation.daappnavigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation.alcoholQuestionnaireScreen
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.core.utils.ONBOARDING_SCREEN_ROUTE
import com.uoa.core.utils.WELCOME_ROUTE
import com.uoa.safedriveafrica.DAAppState
import com.uoa.safedriveafrica.ui.splashscreens.disclaimerScreen
import com.uoa.safedriveafrica.ui.splashscreens.welcomeScreen
import com.uoa.driverprofile.presentation.ui.navigation.drivingTipDetailsScreen
import com.uoa.driverprofile.presentation.ui.navigation.homeScreen

import com.uoa.driverprofile.presentation.ui.navigation.onboardingScreen
import com.uoa.nlgengine.presentation.ui.reportNavigation.filterScreen
import com.uoa.nlgengine.presentation.ui.reportNavigation.reportScreen
import com.uoa.sensor.presentation.ui.sensornavigation.sensorControlScreen


@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DAAppNavHost(
    appState: DAAppState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    startDestination: String = WELCOME_ROUTE
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // Include the entry point screen
        welcomeScreen(navController)
        disclaimerScreen(navController)
        entryPointScreen(navController)

        onboardingScreen(
            navController = navController,
            onShowSnackbar = onShowSnackbar
        )
        homeScreen(navController)
        drivingTipDetailsScreen(navController)
        reportScreen(navController)
        filterScreen(navController)
        sensorControlScreen(navController)

        // Add the Alcohol Questionnaire screen route
        alcoholQuestionnaireScreen(navController)
    }
}