package com.uoa.sensor.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionSettingsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = {
                Text(text = "Permission Required")
            },
            text = {
                Text(text = "To proceed, please enable the required permissions in Settings.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}