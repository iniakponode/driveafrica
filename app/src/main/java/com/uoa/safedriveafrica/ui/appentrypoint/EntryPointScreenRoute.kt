package com.uoa.safedriveafrica.ui.appentrypoint

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.*
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.LAST_QUESTIONNAIRE_DAY
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.driverprofile.presentation.ui.navigation.navigateToHomeScreen
import com.uoa.driverprofile.presentation.ui.navigation.navigateToOnboardingInfoScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun EntryPointScreenRoute(

    navController: NavController
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
    val lastDay = prefs.getString(LAST_QUESTIONNAIRE_DAY, null)
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    // Retained check for daily questionnaire reminders
    val shouldShowQuestionnaire = lastDay != today

    LaunchedEffect(savedProfileId) {
        if (savedProfileId == null) {
            // No profile found, navigate to onboarding screen.
            navController.navigateToOnboardingInfoScreen {
                popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
            }
        } else {
            val profileUuid = try {
                UUID.fromString(savedProfileId)
            } catch (e: IllegalArgumentException) {
                null
            }
            if (profileUuid != null) {
                navController.navigateToHomeScreen(profileUuid) {
                    popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
                }
            } else {
                navController.navigateToOnboardingInfoScreen {
                    popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
                }
            }
        }
    }

    // Display the loading UI while the navigation decision is being made.
//    EntryPointScreen()
}
