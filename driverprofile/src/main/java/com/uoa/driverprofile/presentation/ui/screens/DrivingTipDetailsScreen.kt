package com.uoa.driverprofile.presentation.ui.screens

// Driving Tip Details Screen composable function
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uoa.core.model.DrivingTip
import com.uoa.core.network.Dispatcher
import com.uoa.driverprofile.R
import com.uoa.driverprofile.presentation.viewmodel.DrivingTipsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun DrivingTipDetailsScreen(
    drivingTip: DrivingTip?,
    onBackClick: () -> Unit

) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
            Text(
                text = drivingTip!!.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = drivingTip.meaning ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            drivingTip.penalty?.let {
                Text(
                    text = "Penalty: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            drivingTip.law?.let {
                Text(
                    text = "Law: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = drivingTip.summaryTip ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }


@Composable
fun DrivingTipDetailsScreenRoute(
    navController: NavController,
    tipId: UUID,
    drivingTipsViewModel: DrivingTipsViewModel = hiltViewModel()
) {
    // State holder for the driving tip
    val drivingTipState = remember { mutableStateOf<DrivingTip?>(null) }
    val context = LocalContext.current

    // Side-effect to fetch the driving tip
    LaunchedEffect(tipId) {
        val drivingTip = drivingTipsViewModel.getDrivingTipById(tipId)
        if (drivingTip != null) {
            drivingTipState.value = drivingTip
        } else {
            // Handle the case where the tip is not found
            Toast.makeText(context, "Driving tip not found", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // If the driving tip is loaded, display it
    drivingTipState.value?.let { drivingTip ->
        DrivingTipDetailsScreen(
            drivingTip = drivingTip,
            onBackClick = { navController.popBackStack() }
        )
    } ?: run {
        // Display a loading indicator or placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
