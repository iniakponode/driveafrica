package com.uoa.driverprofile.presentation.ui.navigation

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.DRIVER_PROFILE_ID
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.core.utils.HOME_SCREEN_ROUTE
import com.uoa.driverprofile.presentation.ui.screens.HomeScreenRoute
import java.util.UUID


// Constants for routes
//const val DRIVER_PROFILE_ID = "profileId"
//const val HOME_SCREEN_ROUTE = "homeScreen/{$DRIVER_PROFILE_ID}"

// Navigation function to home screen using NavOptionsBuilder for DSL-style configuration

private const val HOME_BASE_ROUTE = "homeScreen" // no-arg
fun NavController.navigateToHomeScreen(
    profileId: UUID,
    navOptions: NavOptionsBuilder.() -> Unit = {}
) {
    navigate("homeScreen/$profileId", navOptions)
}


fun NavGraphBuilder.homeScreen(
    navController: NavController
) {
    composable(
        route = HOME_SCREEN_ROUTE, // "homeScreen/{profileId}"
        arguments = listOf(navArgument(DRIVER_PROFILE_ID) { type = NavType.StringType })
    ) { backStackEntry ->
        val ctx = LocalContext.current
        val arg = backStackEntry.arguments?.getString(DRIVER_PROFILE_ID)?.trim()

        val resolved = when {
            !arg.isNullOrBlank() -> arg
            else -> ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(DRIVER_PROFILE_ID, null)?.trim()
        }

        val profileId = runCatching { UUID.fromString(resolved) }.getOrNull()
        if (profileId == null) {
            navController.navigate(ENTRYPOINT_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }

            return@composable
        }

        HomeScreenRoute(navController = navController, profileId = profileId)
    }


    composable(HOME_BASE_ROUTE) {
        val ctx = LocalContext.current
        val id = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(DRIVER_PROFILE_ID, null)?.trim()

        if (id != null && runCatching { UUID.fromString(id) }.isSuccess) {
            navController.navigate("homeScreen/$id") {
                popUpTo(HOME_BASE_ROUTE) { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(ENTRYPOINT_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

}

