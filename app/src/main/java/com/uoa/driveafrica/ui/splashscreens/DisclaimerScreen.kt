package com.uoa.driveafrica.ui.splashscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.DISCLAIMER_ROUTE
import com.uoa.core.utils.ENTRYPOINT_ROUTE

/**
 * 2. DISCLAIMER SCREEN
 */
fun NavGraphBuilder.disclaimerScreen(navController: NavController) {
    composable(route = DISCLAIMER_ROUTE) {
        DisclaimerScreenRoute(navController)
    }
}

@Composable
fun DisclaimerScreenRoute(navController: NavController) {
    DisclaimerScreen(
        onContinue = {
            // Navigate to the existing entry point route
            // (E.g. `ENTRYPOINT_ROUTE` from your constants).
            navController.navigate(ENTRYPOINT_ROUTE) {
                popUpTo(DISCLAIMER_ROUTE) { inclusive = true }
            }
        }
    )
}


@Composable
fun DisclaimerScreen(
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Disclaimer",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Driving under the influence of alcohol is never safe." +
                            "This study also aims to understand alcohol-influenced driving behaviour\n " +
                            "to enhance road safety. Please participate responsibly.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                val context = LocalContext.current
                Button(onClick = onContinue) {
                    Text(text = "Continue")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://datahub.safedriveafrica.com/privacy"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}