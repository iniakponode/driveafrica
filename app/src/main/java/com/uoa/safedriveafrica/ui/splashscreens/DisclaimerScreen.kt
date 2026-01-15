package com.uoa.safedriveafrica.ui.splashscreens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.core.utils.DISCLAIMER_ROUTE
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.safedriveafrica.R

fun NavGraphBuilder.disclaimerScreen(navController: NavController) {
    composable(route = DISCLAIMER_ROUTE) {
        DisclaimerScreenRoute(navController)
    }
}

@Composable
fun DisclaimerScreenRoute(navController: NavController) {
    DisclaimerScreen(
        onContinue = {
            navController.navigate(ENTRYPOINT_ROUTE) {
                popUpTo(DISCLAIMER_ROUTE) { inclusive = true }
            }
        }
    )
}

@Composable
fun DisclaimerScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val bullets = listOf(
        stringResource(R.string.disclaimer_bullet_1),
        stringResource(R.string.disclaimer_bullet_2),
        stringResource(R.string.disclaimer_bullet_3)
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.disclaimer_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        bullets.forEach { bullet ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.disclaimer_footer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.agree_continue),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = stringResource(R.string.privacy_policy),
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable {
                            val uri = Uri.parse("https://datahub.safedriveafrica.com/privacy")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                }
            }
        }
    }
}
