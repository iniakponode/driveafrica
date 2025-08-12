package com.uoa.safedriveafrica.presentation.daappnavigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.LAST_QUESTIONNAIRE_DAY
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.driverprofile.presentation.ui.navigation.navigateToOnboardingScreen
import com.uoa.driverprofile.presentation.ui.navigation.navigateToHomeScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun NavGraphBuilder.entryPointScreen(
    navController: NavController
) {
    composable(ENTRYPOINT_ROUTE) {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
        val lastDay = prefs.getString(LAST_QUESTIONNAIRE_DAY, null)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        // Retained for reminder checks
        val shouldShowQuestionnaire = lastDay != today

        // We only run this logic once
        LaunchedEffect(Unit) {
            if (savedProfileId == null) {
                // No profile => go to onboarding
                navController.navigateToOnboardingScreen {
                    popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
                }
            } else {
                val profileUuid = try {
                    java.util.UUID.fromString(savedProfileId)
                } catch (e: IllegalArgumentException) {
                    null
                }
                if (profileUuid != null) {
                    navController.navigateToHomeScreen(profileUuid) {
                        popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
                    }
                } else {
                    navController.navigateToOnboardingScreen {
                        popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
                    }
                }
            }
        }

        // You can show a temporary splash or loading screen here if desired
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
