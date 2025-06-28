package com.uoa.safedriveafrica.ui.splashscreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.uoa.core.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Start a coroutine that waits a few seconds before moving on
    LaunchedEffect(Unit) {
        delay(2000L) // adjust delay as needed
        onSplashFinished()
    }

    // Display the original image in the center of the screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sda_2),
            contentDescription = "Splash Screen Image",
            modifier = Modifier.size(200.dp) // adjust size as needed
        )
    }
}
