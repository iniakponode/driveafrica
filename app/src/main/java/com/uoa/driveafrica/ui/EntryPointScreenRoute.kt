package com.uoa.driveafrica.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation.alcoholQuestionnaireScreen
import com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation.navigateToQuestionnaire
import com.uoa.driverprofile.presentation.ui.navigation.navigateToOnboardingScreen

@Composable
fun EntryPointScreenRoute(

    navController: NavController
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)

    LaunchedEffect(savedProfileId) {
        if (savedProfileId == null) {
            // No profile found, navigate to onboarding screen.
            navController.navigateToOnboardingScreen() {
                popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
            }
        } else {
            // Profile exists, navigate to the questionnaire screen.
            navController.navigateToQuestionnaire {
                popUpTo(ENTRYPOINT_ROUTE) { inclusive = true }
            }
        }
    }

    // Display the loading UI while the navigation decision is being made.
    EntryPointScreen()
}
