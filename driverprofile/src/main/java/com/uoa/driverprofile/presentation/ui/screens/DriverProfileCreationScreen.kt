package com.uoa.driverprofile.presentation.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.core.utils.Constants.Companion.DRIVER_EMAIL_ID
//import com.uoa.driverprofile.presentation.ui.CustomTextField
import com.uoa.driverprofile.presentation.viewmodel.DriverProfileViewModel
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.driverprofile.presentation.ui.navigation.navigateToHomeScreen
//import com.uoa.sensor.services.VehicleMovementServiceUpdate.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

// Composable to let drivers enter their email as well as create a profileId.
// This is the first screen that drivers will see when they open the app.
// The email is used to identify the driver and the profileId is used to store the driver's profile.
// The ProfileId should also be stored in the shared preferences so that the driver can access their profile later.

@Composable
fun DriverProfileCreationScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isError: Boolean,
    onShowSnackbar: suspend (String, String?) -> Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TextField(
            value = email,
            onValueChange = { newEmail ->
                onEmailChange(newEmail)
            },
            label = { Text("Enter Profile ID given to you") },
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
        if (isError) {
            LaunchedEffect(isError) {
                onShowSnackbar("This field cannot be empty", null)
            }
            Text("This field cannot be empty", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                if (email.isNotBlank()) {
                    onSubmit()
                }
            },
            modifier = Modifier.padding(top = 16.dp),
            enabled = !isError
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Profile")
        }
    }
}

@Composable
fun DriverProfileCreationRoute(
    navController: NavController,
    driverProfileViewModel: DriverProfileViewModel = hiltViewModel(),
    onShowSnackbar: suspend (String, String?) -> Boolean
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
    var emailState by rememberSaveable { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Check if the profile ID exists and navigate directly to the home screen
    LaunchedEffect(Unit) {
        if (savedProfileId != null) {
            val profileId = try {
                UUID.fromString(savedProfileId)
            } catch (e: IllegalArgumentException) {
                null
            }
            if (profileId != null) {
                Log.e("ProfileID"," $profileId")
                navController.navigateToHomeScreen(profileId)
            } else {
                // Handle invalid saved profile ID
                prefs.edit().remove(DRIVER_PROFILE_ID).apply()
                prefs.edit().remove(DRIVER_EMAIL_ID).apply()
            }
        }
    }

    val uploadSuccess by driverProfileViewModel.driverProfileUploadSuccess.collectAsState()

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onShowSnackbar("Driver Profile successfully uploaded!", null)
        }
    }

    // Callback when the submit button is clicked
    val onSubmit: () -> Unit = {
        if (emailState.isBlank()) {
            isError = true
        } else {
            val newProfileId = UUID.randomUUID()
            driverProfileViewModel.insertDriverProfile(newProfileId, emailState) { success ->
                if (success) {
                    prefs.edit().putString(DRIVER_PROFILE_ID, newProfileId.toString()).apply()
                    prefs.edit().putString(DRIVER_EMAIL_ID, emailState.toString()).apply()
                    val profId=prefs.getString(DRIVER_PROFILE_ID, null)
                    Log.e("ProfileID"," $profId")
                    navController.navigateToHomeScreen(newProfileId)
                } else {
                    // Show error message on the main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        onShowSnackbar("Failed to create profile", null)
                    }
                }
            }
        }
    }

    // Callback when the email input changes
    val onEmailChange: (String) -> Unit = { newEmail ->
        emailState = newEmail
        isError = newEmail.isBlank()
    }

    // Render the UI
    DriverProfileCreationScreen(
        email = emailState,
        onEmailChange = onEmailChange,
        onSubmit = onSubmit,
        isError = isError,
        onShowSnackbar = onShowSnackbar
    )
}

