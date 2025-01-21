package com.uoa.driverprofile.presentation.ui.screens
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.core.model.DrivingTip
import com.uoa.driverprofile.presentation.ui.composables.TipList
import com.uoa.driverprofile.presentation.ui.navigation.navigateToDrivingTipDetailsScreen
import com.uoa.driverprofile.presentation.viewmodel.DrivingTipsViewModel
import java.util.UUID


@Composable
fun HomeScreen(
    gptDrivingTips: List<DrivingTip>,
    geminiDrivingTips: List<DrivingTip>,
    onDrivingTipClick: (UUID) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Today's Driving Tips",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (gptDrivingTips.isEmpty() && geminiDrivingTips.isEmpty()) {
            Text("No driving tips available for today.")
        } else {
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

    HomeScreen(
        gptDrivingTips = gpt_drivingTips,
        geminiDrivingTips = gemini_tips,
        onDrivingTipClick = onDrivingTipClick
    )
}