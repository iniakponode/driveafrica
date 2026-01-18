package com.uoa.driverprofile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.driverProfile.DriverProfileResponse
import com.uoa.core.apiServices.workManager.enqueueImmediateUploadWork
import com.uoa.core.apiServices.workManager.scheduleDataUploadWork
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.utils.Constants.Companion.DRIVER_EMAIL_ID
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.REGISTRATION_FLEET_CHOICE
import com.uoa.core.utils.Constants.Companion.REGISTRATION_HAS_INVITE_CODE
import com.uoa.core.utils.Constants.Companion.REGISTRATION_INVITE_CODE
import com.uoa.core.utils.SecureCredentialStorage
import com.uoa.driverprofile.R
import com.uoa.driverprofile.domain.usecase.DeleteDriverProfileByEmailUseCase
import com.uoa.driverprofile.domain.usecase.GetDriverProfileByEmailUseCase
import com.uoa.driverprofile.domain.usecase.InsertDriverProfileUseCase
import com.uoa.driverprofile.presentation.model.FleetEnrollmentChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class DriverProfileViewModel @Inject constructor(
    private val insertDriverProfileUseCase: InsertDriverProfileUseCase,
    private val getDriverProfileByEmailUseCase: GetDriverProfileByEmailUseCase,
    private val deleteDriverProfileByEmailUseCase: DeleteDriverProfileByEmailUseCase,
    private val secureCredentialStorage: SecureCredentialStorage,
    private val application: Application,
) : ViewModel() {

    private val _email = MutableLiveData<String>()
    val email: MutableLiveData<String> get() = _email

    private val _profile_id = MutableLiveData<String>()
//    val profile_id: MutableLiveData<String> get() = _profile_id

    private val _driverProfileUploadSuccess = MutableStateFlow(false)
//    val driverProfileUploadSuccess: StateFlow<Boolean> get() = _driverProfileUploadSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _creationMessage = MutableStateFlow<String?>(null)
    val creationMessage: StateFlow<String?> get() = _creationMessage.asStateFlow()

    private val _currentDriverProfile = MutableStateFlow<DriverProfileResponse?>(null)
    val currentDriverProfile: StateFlow<DriverProfileResponse?> get() = _currentDriverProfile.asStateFlow()

    fun clearStatusMessage() {
        _creationMessage.value = null
    }

    fun createDriverProfile(
        email: String,
        password: String,
        fleetChoice: FleetEnrollmentChoice,
        hasInviteCode: Boolean,
        callback: (Boolean, UUID?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val trimmedPassword = password.trim()
            val passwordBytes = trimmedPassword.encodeToByteArray()
            if (passwordBytes.size > MAX_PASSWORD_BYTES) {
                Log.w("DriverProfileViewModel", "Password exceeds $MAX_PASSWORD_BYTES bytes.")
                _driverProfileUploadSuccess.value = false
                _creationMessage.value = application.getString(R.string.onboarding_password_too_long)
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
                _isLoading.value = false
                return@launch
            }
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                _driverProfileUploadSuccess.value = false
                _creationMessage.value = application.getString(
                    if (trimmedEmail.isBlank()) {
                        R.string.onboarding_error_empty
                    } else {
                        R.string.onboarding_error_invalid_email
                    }
                )
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
                _isLoading.value = false
                return@launch
            }
            val profileId = UUID.randomUUID()
            try {
                val localResult = runCatching {
                    val entity = DriverProfileEntity(email = trimmedEmail, driverProfileId = profileId, sync = false)
                    insertDriverProfileUseCase.execute(entity)
                }

                if (localResult.isFailure) {
                    Log.e("DriverProfileViewModel", "Failed to save driver profile locally.", localResult.exceptionOrNull())
                    _driverProfileUploadSuccess.value = false
                    withContext(Dispatchers.Main) {
                    callback(false, null)
                    }
                    _creationMessage.value = "Unable to save profile locally."
                    return@launch
                }

                val prefs = application.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(DRIVER_PROFILE_ID, profileId.toString())
                    .putString(DRIVER_EMAIL_ID, trimmedEmail)
                    .putString(REGISTRATION_FLEET_CHOICE, fleetChoice.name)
                    .putBoolean(REGISTRATION_HAS_INVITE_CODE, hasInviteCode)
                    .apply()
                prefs.edit {
                    remove(REGISTRATION_INVITE_CODE)
                }
                _driverProfileUploadSuccess.value = true
                secureCredentialStorage.saveCredentials(trimmedEmail, trimmedPassword)
                _creationMessage.value = "Profile saved locally and will sync when you're online."
                _currentDriverProfile.value = DriverProfileResponse(profileId, trimmedEmail, false)
                withContext(Dispatchers.Main) { callback(true, profileId) }

            } catch (exception: Exception) {
                Log.e("DriverProfileViewModel", "Failed to create profile.", exception)
                _creationMessage.value = "An unexpected error occurred while creating your profile."
                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

//    fun getDriverProfileByEmail() {
//        viewModelScope.launch {
//            val emailValue = email.value.toString()
//            Log.d("DriverProfileViewModel", "Getting driver profile by email: $emailValue")
//            getDriverProfileByEmailUseCase.execute(emailValue)
//        }
//    }

//    fun deleteDriverProfileByEmail() {
//        viewModelScope.launch {
//            val emailValue = email.value.toString()
//            Log.d("DriverProfileViewModel", "Deleting driver profile by email: $emailValue")
//            deleteDriverProfileByEmailUseCase.execute(emailValue)
//        }
//    }

    fun triggerUploadWork() {
        enqueueImmediateUploadWork(application.applicationContext)
        scheduleDataUploadWork(application.applicationContext)
    }

    companion object {
        private const val MAX_PASSWORD_BYTES = 72
    }
}
