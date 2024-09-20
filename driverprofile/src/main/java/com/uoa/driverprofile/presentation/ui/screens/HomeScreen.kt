package com.uoa.driverprofile.presentation.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.core.model.DrivingTip
import com.uoa.driverprofile.presentation.ui.navigation.navigateToDrivingTipDetailsScreen
import com.uoa.driverprofile.presentation.viewmodel.DrivingTipsViewModel
import java.util.UUID

@Composable
internal fun HomeScreen(
    drivingTips: List<DrivingTip>,
    onDrivingTipClick: (UUID) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Today's Driving Tips",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (drivingTips.isEmpty()) {
            Text("No driving tips available for today.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(drivingTips) { tip ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onDrivingTipClick(tip.tipId)
                        },
                        headlineContent = { Text(tip.title) },
                        supportingContent = { Text(tip.summaryTip ?: "") }
                    )
                }
            }
        }
    }
}


@Composable
fun HomeScreenRoute(
    navController: NavController,
    drivingTipsViewModel: DrivingTipsViewModel = hiltViewModel()
) {
    val drivingTips by drivingTipsViewModel.drivingTips.observeAsState(emptyList())
//    val drivingTips by drivingTipsViewModel.getDrivingTipsForDriver(profileId)
//        .collectAsState(initial = emptyList())
    val onDrivingTipClick: (UUID) -> Unit = { tipId ->
        navController.navigateToDrivingTipDetailsScreen(tipId)
    }

    HomeScreen(
        drivingTips = drivingTips,
        onDrivingTipClick = onDrivingTipClick
    )
}