package com.uoa.alcoholquestionnaire.presentation.ui.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.alcoholquestionnaire.presentation.viewmodel.QuestionnaireViewModel
import com.uoa.core.utils.ALCOHOL_QUESTIONNAIRE_ROUTE
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Resource
import java.util.UUID

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
                val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
                val profileUuid = UUID.fromString(savedProfileId ?: return@LaunchedEffect)

                // Navigate to home screen after success
                navController.navigate("homeScreen/${profileUuid}") {
                    popUpTo(ALCOHOL_QUESTIONNAIRE_ROUTE) {
                        inclusive = true
                    }
                }
            }
            is Resource.Error -> {
                val errorMessage = (uploadState as Resource.Error).message
                onShowSnackbar(errorMessage, null)
            }
            else -> { /* Loading or null state, handle if needed */ }
        }
    }

    AlcoholQuestionnaireScreen(
        onSubmit = { responseMap ->
            questionnaireViewModel.uploadResponseToServer(responseMap)
        },
        onCancel = {
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
