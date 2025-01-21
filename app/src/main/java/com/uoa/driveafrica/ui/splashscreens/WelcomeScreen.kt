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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.DISCLAIMER_ROUTE
import com.uoa.core.utils.WELCOME_ROUTE

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
    // You can display your logo, an image, or any background splash here
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(9.dp),
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
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Optionally show an image or logo at the top
                // Image(painter = painterResource(id = R.drawable.your_logo), contentDescription = null)

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Welcome to the Safe Drive Africa APP",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A PhD Research Project App for encouraging Safe Driving Behaviour in Non-Western Countries - The Nigerian Focused Version 2025." +
                            "This research is domiciled at the University of Aberdeen Scotland United Kingdom.\n" +
                            "Research is conducted by Iniakpokeikiye Peter Thompson\n" +
                            "Phone:+447733610338, +2347045598128, Email: i.thompson.21@abdn.ac.uk\n" +
                            "Supervised by Prof. Ehud Reiter and Dr. Yi Dewei",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(5.dp))
                Button(onClick = onContinue) {
                    Text(text = "Continue")
                }
            }
        }
    }
}