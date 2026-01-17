package com.uoa.driverprofile.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.JOIN_FLEET_ROUTE
import com.uoa.driverprofile.presentation.ui.screens.JoinFleetScreenRoute

fun NavGraphBuilder.joinFleetScreen(navController: NavController) {
    composable(JOIN_FLEET_ROUTE) {
        JoinFleetScreenRoute(navController = navController)
    }
}
