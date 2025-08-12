package com.uoa.alcoholquestionnaire.presentation.ui.screens

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.alcoholquestionnaire.presentation.viewmodel.QuestionnaireViewModel
import com.uoa.core.utils.ALCOHOL_QUESTIONNAIRE_ROUTE
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.LAST_QUESTIONNAIRE_DAY
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Resource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun AlcoholQuestionnaireScreenRoute(
    navController: NavController,
    onShowSnackbar: suspend (String, String?) -> Boolean = { _, _ -> false },
    questionnaireViewModel: QuestionnaireViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val uploadState by questionnaireViewModel.uploadState.observeAsState()

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is Resource.Success -> {
                Log.e("Navigation", uploadState.toString())
                val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
                val profileUuid = UUID.fromString(savedProfileId ?: return@LaunchedEffect)

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
        onSubmit = { responseMap ->
            questionnaireViewModel.saveAndAttemptUpload(responseMap)
        },
        onSkip = {
            val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
            val profileUuid = UUID.fromString(savedProfileId ?: return@AlcoholQuestionnaireScreen)

            navController.navigate("homeScreen/${profileUuid}") {
                popUpTo(ALCOHOL_QUESTIONNAIRE_ROUTE) {
                    inclusive = true
                }
            }
        }
    )
}
