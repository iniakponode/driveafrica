package com.uoa.driverprofile.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uoa.core.utils.ONBOARDING_FORM_ROUTE
import com.uoa.core.utils.ONBOARDING_SCREEN_ROUTE
import com.uoa.core.utils.REGISTRATION_CHOICE_ROUTE
import com.uoa.driverprofile.presentation.model.RegistrationMode
import com.uoa.driverprofile.presentation.ui.screens.DriverProfileCreationRoute
import com.uoa.driverprofile.presentation.ui.screens.OnboardingInfoRoute
import com.uoa.driverprofile.presentation.ui.screens.RegistrationChoiceRoute

fun NavController.navigateToOnboardingInfoScreen(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    if (graph.findNode(ONBOARDING_SCREEN_ROUTE) != null) {
        navigate(ONBOARDING_SCREEN_ROUTE, navOptions)
    }
}

fun NavController.navigateToRegistrationChoiceScreen(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    if (graph.findNode(REGISTRATION_CHOICE_ROUTE) != null) {
        navigate(REGISTRATION_CHOICE_ROUTE, navOptions)
    }
}

fun NavController.navigateToOnboardingFormScreen(
    registrationMode: RegistrationMode,
    navOptions: NavOptionsBuilder.() -> Unit = {}
) {
    if (graph.findNode(ONBOARDING_FORM_ROUTE) != null) {
        navigate("${ONBOARDING_FORM_ROUTE}?mode=${registrationMode.name}", navOptions)
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
                navController.navigateToRegistrationChoiceScreen {
                    popUpTo(ONBOARDING_SCREEN_ROUTE) { inclusive = true }
                }
            }
        )
    }
}

fun NavGraphBuilder.registrationChoiceScreen(
    navController: NavController
) {
    composable(route = REGISTRATION_CHOICE_ROUTE) {
        RegistrationChoiceRoute(navController = navController)
    }
}

fun NavGraphBuilder.onboardingFormScreen(
    navController: NavController,
    onShowSnackbar: suspend (String, String?) -> Boolean
) {
    composable(
        route = "${ONBOARDING_FORM_ROUTE}?mode={mode}",
        arguments = listOf(
            navArgument("mode") {
                defaultValue = RegistrationMode.Email.name
            }
        )
    ) { backStackEntry ->
        val modeArg = backStackEntry.arguments?.getString("mode")
        val registrationMode = RegistrationMode.fromRoute(modeArg)
        DriverProfileCreationRoute(
            navController = navController,
            onShowSnackbar = onShowSnackbar,
            registrationMode = registrationMode
        )
    }
}




