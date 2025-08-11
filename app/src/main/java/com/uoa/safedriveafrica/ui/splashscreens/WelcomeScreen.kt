package com.uoa.safedriveafrica.ui.splashscreens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.DISCLAIMER_ROUTE
import com.uoa.core.utils.WELCOME_ROUTE
import com.uoa.core.R

/**
 * 1. WELCOME SCREEN
 */
fun NavGraphBuilder.welcomeScreen(navController: NavController) {
    composable(route = WELCOME_ROUTE) {
        WelcomeScreenRoute(navController)
    }
}

@Composable
fun WelcomeScreenRoute(navController: NavController) {
    // You can keep a simple UI or route logic here
    WelcomeScreen(
        onContinue = {
            navController.navigate(DISCLAIMER_ROUTE) {
                popUpTo(WELCOME_ROUTE) { inclusive = true }
            }
        }
    )
}

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    // Wrapping the content in a Box and making the inner content scrollable.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            // The Column now scrolls if the content overflows.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Display the original image used to create your icons.
                Image(
                    painter = painterResource(id = R.drawable.sda_2),
                    contentDescription = "Splash Screen Image",
                    modifier = Modifier
                        .size(100.dp)
                        .height(180.dp)
                        .padding(bottom = 2.dp)
                )
//                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "Welcome to the Safe Drive Africa APP",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A PhD Research Project App for encouraging Safe Driving Behaviour in Non-Western Countries - The Nigerian Focused Version 2025.\n" +
                            "This research is domiciled at the University of Aberdeen, Scotland, United Kingdom.\n" +
                            "Research is conducted by Iniakpokeikiye Peter Thompson\n" +
                            "Phone: +447733610338, +2347045598128, Email: i.thompson.21@abdn.ac.uk\n" +
                            "Supervised by Prof. Ehud Reiter and Dr. Yi Dewei",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(5.dp))
                Button(onClick = onContinue) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Continue")
                }
            }
        }
    }
}