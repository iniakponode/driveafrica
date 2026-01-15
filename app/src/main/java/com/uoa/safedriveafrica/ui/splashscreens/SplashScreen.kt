package com.uoa.safedriveafrica.ui.splashscreens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.R as CoreR
import com.uoa.safedriveafrica.R as AppR
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.SPLASH_ROUTE
import com.uoa.driverprofile.presentation.ui.navigation.navigateToHomeScreen
import com.uoa.driverprofile.presentation.ui.navigation.navigateToOnboardingInfoScreen
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val statuses = listOf(
        stringResource(AppR.string.splash_status_connect),
        stringResource(AppR.string.splash_status_assignment),
        stringResource(AppR.string.splash_status_ready)
    )
    val progress = remember { Animatable(0f) }
    val currentStageIndex = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        statuses.forEachIndexed { index, _ ->
            currentStageIndex.value = index
            progress.animateTo(
                targetValue = (index + 1) / statuses.size.toFloat(),
                animationSpec = tween(durationMillis = 600)
            )
            delay(500)
        }
        delay(600)
        onSplashFinished()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = CoreR.drawable.sda_2),
                        contentDescription = "Safe Drive Africa logo",
                        modifier = Modifier.size(140.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(AppR.string.splash_tagline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(AppR.string.splash_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 12.dp),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                )
                StatusBadge(
                    text = statuses[currentStageIndex.value],
                    active = true
                )
            }
            }
            Spacer(modifier = Modifier.height(16.dp))
            StatusRow(statuses, currentStageIndex.value)

            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(AppR.string.splash_backend_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(AppR.string.welcome_policy_reassurance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(statuses: List<String>, activeIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEachIndexed { index, text ->
            StatusBadge(text = text, active = index == activeIndex)
        }
    }
}

@Composable
private fun StatusBadge(text: String, active: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Box(
            modifier = Modifier
                .height(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

fun NavGraphBuilder.splashScreen(navController: NavController) {
    composable(route = SPLASH_ROUTE) {
        SplashScreen(onSplashFinished = {
            val context = navController.context
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
            val profileUuid = savedProfileId?.let { id ->
                try {
                    UUID.fromString(id)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            if (profileUuid != null) {
                navController.navigateToHomeScreen(profileUuid) {
                    popUpTo(SPLASH_ROUTE) { inclusive = true }
                }
            } else {
                navController.navigateToOnboardingInfoScreen {
                    popUpTo(SPLASH_ROUTE) { inclusive = true }
                }
            }
        })
    }
}
