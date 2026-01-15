package com.uoa.driverprofile.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.ONBOARDING_FORM_ROUTE
import com.uoa.core.utils.ONBOARDING_SCREEN_ROUTE
import com.uoa.driverprofile.presentation.ui.screens.DriverProfileCreationRoute
import com.uoa.driverprofile.presentation.ui.screens.OnboardingInfoRoute

fun NavController.navigateToOnboardingInfoScreen(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    if (graph.findNode(ONBOARDING_SCREEN_ROUTE) != null) {
        navigate(ONBOARDING_SCREEN_ROUTE, navOptions)
    }
}

fun NavController.navigateToOnboardingFormScreen(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    if (graph.findNode(ONBOARDING_FORM_ROUTE) != null) {
        navigate(ONBOARDING_FORM_ROUTE, navOptions)
    }
}

/**
 * Add a composable function to the NavGraphBuilder that will create the OnboardDriverScreen composable
 * and pass the required arguments to it.
 *
 * @param navController The NavController that will be used to navigate to other destinations
 * @param onShowSnackbar The lambda function that will be used to show a snackbar
 */

fun NavGraphBuilder.onboardingInfoScreen(
    navController: NavController,
    onShowSnackbar: suspend (String, String?) -> Boolean
) {
    composable(route = ONBOARDING_SCREEN_ROUTE) {
        OnboardingInfoRoute(
            onContinue = {
                navController.navigateToOnboardingFormScreen {
                    popUpTo(ONBOARDING_SCREEN_ROUTE) { inclusive = true }
                }
            }
        )
    }
}

fun NavGraphBuilder.onboardingFormScreen(
    navController: NavController,
    onShowSnackbar: suspend (String, String?) -> Boolean
) {
    composable(route = ONBOARDING_FORM_ROUTE) {
        DriverProfileCreationRoute(
            navController = navController,
            onShowSnackbar = onShowSnackbar
        )
    }
}




