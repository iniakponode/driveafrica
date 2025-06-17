package com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.uoa.alcoholquestionnaire.presentation.ui.screens.AlcoholQuestionnaireScreenRoute
import com.uoa.core.utils.ALCOHOL_QUESTIONNAIRE_ROUTE

fun NavController.navigateToQuestionnaire(builder: NavOptionsBuilder.() -> Unit = {}) {
    this.navigate("alcoholQuestionnaire", builder)
}

fun NavGraphBuilder.alcoholQuestionnaireScreen(navController: NavController) {
    composable(route = ALCOHOL_QUESTIONNAIRE_ROUTE) {
        AlcoholQuestionnaireScreenRoute(navController = navController)
    }
}
