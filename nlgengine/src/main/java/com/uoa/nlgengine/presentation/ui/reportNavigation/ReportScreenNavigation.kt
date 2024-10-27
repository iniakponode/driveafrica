package com.uoa.nlgengine.presentation.ui.reportNavigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uoa.nlgengine.presentation.ui.ReportScreenRoute
import com.uoa.nlgengine.util.PeriodType

const val REPORT_SCREEN_ROUTE = "reportScreen"


fun NavGraphBuilder.reportScreen(navController: NavController) {
    composable(
        route = "$REPORT_SCREEN_ROUTE/{startDate}/{endDate}/{periodType}",
        arguments = listOf(
            navArgument("startDate") { type = NavType.LongType; defaultValue = 0L },
            navArgument("endDate") { type = NavType.LongType; defaultValue = 0L },
            navArgument("periodType") { type = NavType.StringType; defaultValue = "NONE" }
        )
    ) { backStackEntry ->
        val startDate = backStackEntry.arguments?.getLong("startDate") ?: 0L
        val endDate = backStackEntry.arguments?.getLong("endDate") ?: 0L
        val periodTypeName = backStackEntry.arguments?.getString("periodType") ?: "NONE"
        val periodType = PeriodType.valueOf(periodTypeName)
        // Optionally decide the screen based on parameters' values
        if (startDate != 0L && endDate != 0L) {
            // Load report based on date range
            ReportScreenRoute(
                navController = navController,
                periodType = periodType,
                startDate = startDate,
                endDate = endDate,
                // Provide ViewModels specific to ReportScreen
                chatGPTViewModel = hiltViewModel(backStackEntry),
                unsafeBehavioursViewModel = hiltViewModel(backStackEntry),
                locationAddressViewModel = hiltViewModel(backStackEntry)
            )
        }
        else if (startDate == 0L && endDate == 0L && periodType == PeriodType.LAST_TRIP) {
            // Load report based on data source
            ReportScreenRoute(
                navController = navController,
                startDate = 0L,
                endDate = 0L,
                periodType = periodType,
                chatGPTViewModel = hiltViewModel(),
                unsafeBehavioursViewModel = hiltViewModel(),
                locationAddressViewModel = hiltViewModel()
            )
        }
        else{
            // Load report based on data source
            ReportScreenRoute(
                navController = navController,
                startDate = 0L,
                endDate = 0L,
                periodType = periodType,
                chatGPTViewModel = hiltViewModel(),
                unsafeBehavioursViewModel = hiltViewModel(),
                locationAddressViewModel = hiltViewModel()
            )
        }
        }

}