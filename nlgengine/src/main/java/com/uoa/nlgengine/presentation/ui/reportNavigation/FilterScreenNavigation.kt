package com.uoa.nlgengine.presentation.ui.reportNavigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.nlgengine.presentation.ui.FilterScreenRoute
import com.uoa.core.utils.FILTER_SCREEN_ROUTE


fun NavGraphBuilder.filterScreen(navController: NavController) {
    composable(FILTER_SCREEN_ROUTE) {
        FilterScreenRoute(navController)
    }
}