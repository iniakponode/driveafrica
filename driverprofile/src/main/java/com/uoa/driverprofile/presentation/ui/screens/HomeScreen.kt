package com.uoa.driverprofile.presentation.ui.screens
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation.navigateToQuestionnaire
import com.uoa.core.model.DrivingTip
import com.uoa.core.utils.Constants.Companion.DRIVER_EMAIL_ID
import com.uoa.core.utils.Constants.Companion.LAST_QUESTIONNAIRE_DAY
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.driverprofile.presentation.ui.composables.TipList
import com.uoa.driverprofile.presentation.ui.navigation.navigateToDrivingTipDetailsScreen
import com.uoa.driverprofile.presentation.viewmodel.DrivingTipsViewModel
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE
import com.uoa.core.utils.FILTER_SCREEN_ROUTE
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun HomeScreen(
    gptDrivingTips: List<DrivingTip>,
    geminiDrivingTips: List<DrivingTip>,
    onDrivingTipClick: (UUID) -> Unit,
    onRecordTripClick: () -> Unit,
    onViewReportsClick: () -> Unit,
    onQuestionnaireClick: () -> Unit,
    showReminder: Boolean,
    onDismissReminder: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedEmail = prefs.getString(DRIVER_EMAIL_ID, null)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (showReminder) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fill today's alcohol check-in?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = onQuestionnaireClick) { Text("Fill in") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismissReminder) { Text("Skip for now") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = "Hi ${savedEmail}!\nYour Driving Tips Today",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (gptDrivingTips.isEmpty() && geminiDrivingTips.isEmpty()) {
            Text("No driving tips available for today.")
        } else {
//            To be activated in second phase of app evaluation
            if (gptDrivingTips.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))
                TipList(
                    tips = gptDrivingTips,
                    source = "GPT",
                    onDrivingTipClick = onDrivingTipClick
                )
            }


            if (geminiDrivingTips.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))
                TipList(
                    tips = geminiDrivingTips,
                    source = "Gemini",
                    onDrivingTipClick = onDrivingTipClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onQuestionnaireClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Assessment, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Daily Alcohol Questionnaire")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRecordTripClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Record Trip")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onViewReportsClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "View Reports")
        }
    }
}





@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun HomeScreenRoute(
    navController: NavController,
    drivingTipsViewModel: DrivingTipsViewModel = hiltViewModel(),
    profileId: UUID
) {
    val gpt_drivingTips by drivingTipsViewModel.gptDrivingTips.observeAsState(emptyList())
    val gemini_tips by drivingTipsViewModel.geminiDrivingTips.observeAsState(emptyList())
    Log.d("HomeScreenRoute", "Observed GPT ${gpt_drivingTips.size} driving tips")
    Log.d("HomeScreenRoute", "Observed Gemini ${gemini_tips.size} driving tips")

    val onDrivingTipClick: (UUID) -> Unit = { tipId ->
        Log.d("HomeScreenRoute", "Navigating to details screen for tip: $tipId")
        navController.navigateToDrivingTipDetailsScreen(tipId)
    }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastDay = prefs.getString(LAST_QUESTIONNAIRE_DAY, null)
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    var showReminder by remember { mutableStateOf(lastDay != today) }

    HomeScreen(
        gptDrivingTips = gpt_drivingTips,
        geminiDrivingTips = gemini_tips,
        onDrivingTipClick = onDrivingTipClick,
        onRecordTripClick = { navController.navigate(SENSOR_CONTROL_SCREEN_ROUTE) },
        onViewReportsClick = { navController.navigate(FILTER_SCREEN_ROUTE) },
        onQuestionnaireClick = { navController.navigateToQuestionnaire() },
        showReminder = showReminder,
        onDismissReminder = { showReminder = false }
    )
}