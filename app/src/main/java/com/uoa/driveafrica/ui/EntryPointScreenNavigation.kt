package com.uoa.driveafrica.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.ENTRYPOINT_ROUTE

fun NavGraphBuilder.entryPointScreen(navController: NavController) {
    composable(route = ENTRYPOINT_ROUTE) {
        EntryPointScreenRoute(navController = navController)
    }
}
