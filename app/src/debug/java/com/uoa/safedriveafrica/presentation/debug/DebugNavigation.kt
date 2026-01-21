package com.uoa.safedriveafrica.presentation.debug

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.uoa.core.utils.TRIP_ML_DEBUG_ROUTE

fun NavGraphBuilder.addDebugRoutes(navController: NavHostController) {
    composable(TRIP_ML_DEBUG_ROUTE) {
        TripMlDebugRoute(onNavigateBack = { navController.popBackStack() })
    }
}
