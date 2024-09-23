package com.uoa.driverprofile.presentation.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uoa.driverprofile.presentation.ui.screens.DrivingTipDetailsScreenRoute
import com.uoa.driverprofile.presentation.viewmodel.DrivingTipsViewModel
import java.util.UUID

// Driving Tip Details Screen Navigation graph

const val DRIVING_TIP_ID = "tipId"
const val DRIVING_TIP_DETAIL_ROUTE = "DrivingTipDetailsScreen/{$DRIVING_TIP_ID}"

fun NavController.navigateToDrivingTipDetailsScreen(tipId: UUID, navOptions: NavOptions? = null) {
    navigate("drivingTipDetailsScreen/$tipId", navOptions)
}

/**
 * Add a composable function to the NavGraphBuilder that will create the DrivingTipDetailsScreen composable
 * and pass the required arguments to it.
 *
 * @param navController The NavController that will be used to navigate to other destinations
 * @param tipID The ID of the driving tip
 */
fun NavGraphBuilder.drivingTipDetailsScreen(navController: NavController) {
    composable(
        route = DRIVING_TIP_DETAIL_ROUTE,
        arguments = listOf(navArgument(DRIVING_TIP_ID) { type = NavType.StringType })
    ) { backStackEntry ->
        val tipIDString = backStackEntry.arguments?.getString(DRIVING_TIP_ID) ?: ""
        val tipID = try {
            UUID.fromString(tipIDString)
        } catch (e: IllegalArgumentException) {
            UUID.randomUUID() // or handle the error appropriately
        }
        val drivingTipsViewModel: DrivingTipsViewModel = hiltViewModel(backStackEntry)
        DrivingTipDetailsScreenRoute(navController,tipID, drivingTipsViewModel)
    }
}