package com.uoa.driverprofile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.auth.AuthResponse
import com.uoa.core.apiServices.models.auth.FleetAssignmentResponse
import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.apiServices.models.auth.LoginRequest
import com.uoa.core.apiServices.models.auth.RegisterRequest
import com.uoa.core.apiServices.services.auth.AuthRepository
import com.uoa.core.apiServices.workManager.scheduleDataUploadWork
import com.uoa.core.apiServices.workManager.enqueueImmediateUploadWork
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.utils.Constants
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.Resource
import com.uoa.core.utils.SecureCredentialStorage
import com.uoa.core.network.NetworkMonitor
import com.uoa.core.database.repository.DriverProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import java.util.Locale
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val secureCredentialStorage: SecureCredentialStorage,
    private val networkMonitor: NetworkMonitor,
    private val driverProfileRepository: DriverProfileRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    fun register(driverProfileId: UUID, email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            secureCredentialStorage.saveCredentials(email, password)
            val isOnline = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
            if (!isOnline) {
                enqueueImmediateUploadWork(getApplication())
                scheduleDataUploadWork(getApplication())
                _state.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "No internet right now. We'll finish registration when you're back online."
                    )
                }
                return@launch
            }
            PreferenceUtils.setRegistrationPending(getApplication(), true)
            PreferenceUtils.setRegistrationCompleted(getApplication(), false)
            val request = RegisterRequest(
                driverProfileId = driverProfileId.toString(),
                email = email,
                password = password,
                sync = true
            )
            try {
                when (val result = authRepository.registerDriver(request)) {
                    is Resource.Success -> {
                        markProfileSynced(driverProfileId, email)
                PreferenceUtils.setRegistrationCompleted(getApplication(), true)
                scheduleDataUploadWork(getApplication())
                handleSuccess(result.data, email)
            }
            is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    }
                    is Resource.Loading -> Unit
                }
            } catch (_: IOException) {
                enqueueImmediateUploadWork(getApplication())
                scheduleDataUploadWork(getApplication())
                _state.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "No internet right now. We'll finish registration when you're back online."
                    )
                }
            } finally {
                PreferenceUtils.setRegistrationPending(getApplication(), false)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            secureCredentialStorage.saveCredentials(email, password)
            when (val result = authRepository.loginDriver(LoginRequest(email, password))) {
                is Resource.Success -> {
                    scheduleDataUploadWork(getApplication())
                    handleSuccess(result.data, email)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun markProfileSynced(driverProfileId: UUID, email: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                driverProfileRepository.updateDriverProfileByEmail(driverProfileId, true, email)
            }.onFailure { throwable ->
                Log.w("AuthViewModel", "Failed to mark driver profile synced: ${throwable.localizedMessage}")
            }
        }
    }

    private suspend fun handleSuccess(response: AuthResponse, email: String) {
        val profileId = response.driverProfile?.id ?: runCatching {
            UUID.fromString(response.driverProfileId ?: "")
        }.getOrNull()

        if (profileId == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Unable to determine driver profile id."
                )
            }
            return
        }

        ensureLocalProfile(profileId, email)
        persistDriverProfile(profileId, email)

        val fleetAssignment = response.fleetAssignment
        val fleetStatusValue = response.fleetStatus?.status?.lowercase(Locale.ROOT)
        val message = if (fleetAssignment != null) {
            val fleetName = fleetAssignment.fleetName
            if (fleetName.isNullOrBlank()) "Assigned to a fleet." else "Assigned to $fleetName"
        } else if (fleetStatusValue == "pending") {
            "Join request pending"
        } else {
            "Logged in successfully."
        }

        _state.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                successMessage = message,
                fleetAssignment = response.fleetAssignment,
                fleetStatus = response.fleetStatus
            )
        }

        _events.emit(AuthEvent.Authenticated(profileId))
    }

    private suspend fun ensureLocalProfile(profileId: UUID, email: String) {
        withContext(Dispatchers.IO) {
            val existingById = runCatching {
                driverProfileRepository.getDriverProfileById(profileId)
            }.getOrNull()
            if (existingById != null) {
                if (existingById.email != email) {
                    driverProfileRepository.updateDriverProfile(
                        DriverProfileEntity(driverProfileId = profileId, email = email, sync = true)
                    )
                }
                return@withContext
            }

            val existingByEmail = runCatching {
                driverProfileRepository.getDriverProfileByEmail(email)
            }.getOrNull()
            if (existingByEmail != null) {
                driverProfileRepository.updateDriverProfileByEmail(profileId, true, email)
            } else {
                driverProfileRepository.insertDriverProfile(
                    DriverProfileEntity(driverProfileId = profileId, email = email, sync = true)
                )
            }
        }
    }

    private fun persistDriverProfile(driverProfileId: UUID, email: String) {
        val prefs = getApplication<Application>().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(Constants.DRIVER_PROFILE_ID, driverProfileId.toString())
                .putString(Constants.DRIVER_EMAIL_ID, email)
        }
    }
}

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val fleetAssignment: FleetAssignmentResponse? = null,
    val fleetStatus: FleetStatusResponse? = null
)

sealed class AuthEvent {
    data class Authenticated(val driverProfileId: UUID) : AuthEvent()
}
