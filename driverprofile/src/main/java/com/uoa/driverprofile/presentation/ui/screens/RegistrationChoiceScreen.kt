package com.uoa.driverprofile.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.uoa.driverprofile.R
import com.uoa.driverprofile.presentation.model.RegistrationMode
import com.uoa.driverprofile.presentation.ui.navigation.navigateToOnboardingFormScreen

@Composable
fun RegistrationChoiceRoute(
    navController: NavController
) {
    RegistrationChoiceScreen(
        onInviteCode = {
            navController.navigateToOnboardingFormScreen(RegistrationMode.InviteCode)
        },
        onEmail = {
            navController.navigateToOnboardingFormScreen(RegistrationMode.Email)
        }
    )
}

@Composable
private fun RegistrationChoiceScreen(
    onInviteCode: () -> Unit,
    onEmail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.registration_choice_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.registration_choice_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        RegistrationChoiceCard(
            title = stringResource(R.string.registration_choice_invite_title),
            body = stringResource(R.string.registration_choice_invite_body),
            actionLabel = stringResource(R.string.registration_choice_invite_action),
            onClick = onInviteCode
        )

        RegistrationChoiceCard(
            title = stringResource(R.string.registration_choice_email_title),
            body = stringResource(R.string.registration_choice_email_body),
            actionLabel = stringResource(R.string.registration_choice_email_action),
            onClick = onEmail
        )
    }
}

@Composable
private fun RegistrationChoiceCard(
    title: String,
    body: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = actionLabel)
            }
        }
    }
}
