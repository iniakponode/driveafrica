package com.uoa.driverprofile.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.uoa.driverprofile.presentation.ui.screens.HomeScreenRoute
import java.util.UUID


// Constants for routes
const val DRIVER_PROFILE_ID = "profileId"
const val HOME_SCREEN_ROUTE = "homeScreen/{$DRIVER_PROFILE_ID}"

// Navigation function to home screen
fun NavController.navigateToHomeScreen(profileId: UUID, navOptions: NavOptions? = null) {
    navigate("homeScreen/$profileId", navOptions)
}


fun NavGraphBuilder.homeScreen(
    navController: NavController
) {
    composable(
        route = HOME_SCREEN_ROUTE,
        arguments = listOf(navArgument(DRIVER_PROFILE_ID) { type = NavType.StringType })
    ) { backStackEntry ->
        val profileIdString = backStackEntry.arguments?.getString(DRIVER_PROFILE_ID) ?: ""
        val profileId = try {
            UUID.fromString(profileIdString)
        } catch (e: IllegalArgumentException) {
            // Handle invalid profile ID
            navController.popBackStack()
            return@composable
        }
        HomeScreenRoute(
            navController = navController,
        )
    }
}

