package com.uoa.driverprofile.presentation.ui.screens

// Driving Tip Details Screen composable function
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    if (drivingTip == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Driving tip unavailable",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to list of tips",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = drivingTip.title,
                    style = MaterialTheme.typography.headlineMedium
                        .copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = drivingTip.meaning ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                drivingTip.penalty?.let {
                    Text(
                        text = "Penalty: $it",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                drivingTip.fine?.let {
                    Text(
                        text = "Fine: $it",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                drivingTip.law?.let {
                    Text(
                        text = "Law: $it",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = drivingTip.summaryTip ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
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
        drivingTipState.value = drivingTip
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
