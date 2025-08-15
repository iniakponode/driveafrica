package com.uoa.driverprofile.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.ONBOARDING_SCREEN_ROUTE
import com.uoa.driverprofile.presentation.ui.screens.DriverProfileCreationRoute

//const val ONBOARDING_SCREEN_ROUTE = "onboardingScreen"

fun NavController.navigateToOnboardingScreen(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    if (graph.findNode(ONBOARDING_SCREEN_ROUTE) != null) {
        navigate(ONBOARDING_SCREEN_ROUTE, navOptions)
    }
}

/**
 * Add a composable function to the NavGraphBuilder that will create the OnboardDriverScreen composable
 * and pass the required arguments to it.
 *
 * @param navController The NavController that will be used to navigate to other destinations
 * @param onShowSnackbar The lambda function that will be used to show a snackbar
 */

const val PREFS_NAME = "prefs"
const val DRIVER_PROF_ID = "profileId"

fun NavGraphBuilder.onboardingScreen(
    navController: NavController,
    onShowSnackbar: suspend (String, String?) -> Boolean
) {
    composable(route = ONBOARDING_SCREEN_ROUTE) {
        DriverProfileCreationRoute(
            navController = navController,
            onShowSnackbar = onShowSnackbar
        )
    }
}




