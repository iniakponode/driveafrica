package com.uoa.alcoholquestionnaire.presentation.ui.screens

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import com.uoa.alcoholquestionnaire.presentation.viewmodel.QuestionnaireViewModel
import com.uoa.core.utils.ALCOHOL_QUESTIONNAIRE_ROUTE
import com.uoa.core.utils.ONBOARDING_SCREEN_ROUTE
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.LAST_QUESTIONNAIRE_DAY
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Resource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun AlcoholQuestionnaireScreenRoute(
    navController: NavController,
    onShowSnackbar: suspend (String, String?) -> Boolean = { _, _ -> false },
    questionnaireViewModel: QuestionnaireViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)

    if (profileIdString.isNullOrEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate(ONBOARDING_SCREEN_ROUTE)
        }
        return
    }

    val profileUuid = runCatching { UUID.fromString(profileIdString) }
        .getOrElse {
            prefs.edit().remove(DRIVER_PROFILE_ID).apply()
            LaunchedEffect(Unit) { navController.navigate(ONBOARDING_SCREEN_ROUTE) }
            return
        }

    val uploadState by questionnaireViewModel.uploadState.observeAsState()

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is Resource.Success -> {
                Log.e("Navigation", uploadState.toString())

                val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                prefs.edit().putString(LAST_QUESTIONNAIRE_DAY, today).apply()

                // Navigate to home screen after success
                navController.navigate("homeScreen/${profileUuid}") {
                    popUpTo(ALCOHOL_QUESTIONNAIRE_ROUTE) {
                        inclusive = true
                    }
                }
            }
            is Resource.Error -> {
                val errorMessage = (uploadState as Resource.Error).message
                Log.e("Navigation", uploadState.toString())
                onShowSnackbar(errorMessage, null)
            }
            else -> { /* Loading or null state, handle if needed */ }
        }
    }

    AlcoholQuestionnaireScreen(
        profileId = profileUuid,
        onSubmit = { responseMap ->
            questionnaireViewModel.saveAndAttemptUpload(responseMap)
        },
        onSkip = {
            navController.navigate("homeScreen/${profileUuid}") {
                popUpTo(ALCOHOL_QUESTIONNAIRE_ROUTE) {
                    inclusive = true
                }
            }
        }
    )
}
