package com.uoa.safedriveafrica.presentation.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun HelpRoute() {
    val context = LocalContext.current
    val supportEmail = "support@safedriveafrica.com"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Help",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        HelpSection(
            title = "Getting started",
            bullets = listOf(
                "Install and open the app on Android 8.0+.",
                "Complete the welcome and disclaimer screens.",
                "Allow required permissions to continue."
            )
        )

        HelpSection(
            title = "Permissions",
            bullets = listOf(
                "Location and activity recognition are required to detect trips.",
                "Foreground service keeps trip recording active.",
                "Notifications show trip status and upload progress (Android 13+ prompts at onboarding)."
            )
        )

        HelpSection(
            title = "Register or log in",
            bullets = listOf(
                "New users: choose a fleet option, enter email + password, tap Register.",
                "Returning users: enter email + password, tap Log in.",
                "First login on a fresh device requires internet."
            )
        )

        HelpSection(
            title = "Offline login",
            bullets = listOf(
                "Offline login works only after a successful online login on this device.",
                "If a cached password exists, you must use the saved email and password.",
                "If no cached password exists, a local profile must already be on the device."
            )
        )

        HelpSection(
            title = "Trip recording",
            bullets = listOf(
                "Open Record Trip to start or stop a trip manually.",
                "Auto Trip Detection can start and stop trips in the background.",
                "If monitoring is paused, log out and log back in to hydrate your local profile."
            )
        )

        HelpSection(
            title = "Reports and tips",
            bullets = listOf(
                "Open Reports, select a period, then tap Generate Report.",
                "Driving tips appear on the Home screen based on recent trips."
            )
        )

        HelpSection(
            title = "Daily alcohol questionnaire",
            bullets = listOf(
                "Complete the questionnaire once per day from the Home screen.",
                "Responses help improve safety analysis and reporting."
            )
        )

        HelpSection(
            title = "Settings and sync",
            bullets = listOf(
                "Enable Auto Trip Detection and adjust sensitivity.",
                "Allow Metered Uploads if you want to sync over mobile data.",
                "Manual Sync uploads pending data immediately."
            )
        )

        HelpSection(
            title = "Account and data deletion",
            bullets = listOf(
                "Copy your Driver Profile ID from Settings.",
                "Email support with your ID to request deletion."
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Support",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = supportEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$supportEmail")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Email")
                }
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, bullets: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            bullets.forEach { bullet ->
                Text(
                    text = "- $bullet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
