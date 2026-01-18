package com.uoa.driverprofile.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.services.fleetApiService.DriverFleetApiRepository
import com.uoa.core.apiServices.workManager.enqueueImmediateUploadWork
import com.uoa.core.apiServices.workManager.scheduleDataUploadWork
import com.uoa.core.network.NetworkMonitor
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.REGISTRATION_INVITE_CODE
import com.uoa.core.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class JoinFleetViewModel @Inject constructor(
    private val driverFleetApiRepository: DriverFleetApiRepository,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(JoinFleetUiState())
    val state = _state.asStateFlow()

    private val _refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    fun submitJoinRequest(code: String) {
        val normalizedCode = code.trim().uppercase(Locale.ROOT)
        if (!isValidInviteCode(normalizedCode)) {
            _state.update {
                it.copy(
                    errorMessage = "Invite code format appears incorrect.",
                    success = false
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, success = false) }
            val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
            if (!online) {
                queueInviteCodeRequest(normalizedCode)
                return@launch
            }
            when (val result = driverFleetApiRepository.joinFleet(normalizedCode)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            success = true,
                            errorMessage = null,
                            queuedOffline = false,
                            fleetName = result.data.fleetName,
                            requestStatus = result.data.status
                        )
                    }
                    _refreshTrigger.tryEmit(Unit)
                }
                is Resource.Error -> {
                    val message = result.message ?: ""
                    if (isNetworkError(message)) {
                        queueInviteCodeRequest(normalizedCode)
                        return@launch
                    }
                    val pending = message.lowercase(Locale.ROOT).contains("pending")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (pending) null else result.message,
                            success = pending || false,
                            queuedOffline = false,
                            requestStatus = if (pending) "pending" else it.requestStatus
                        )
                    }
                    if (pending) {
                        _refreshTrigger.tryEmit(Unit)
                    }
                }
                else -> Unit
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(errorMessage = null, success = false, queuedOffline = false) }
    }

    fun joinWithCode(code: String) {
        val normalizedCode = code.trim().uppercase(Locale.ROOT)
        if (!isValidInviteCode(normalizedCode)) {
            _state.update {
                it.copy(
                    errorMessage = "Invite code format appears incorrect.",
                    success = false
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, success = false) }
            when (val result = driverFleetApiRepository.joinWithCode(normalizedCode)) {
                is Resource.Success -> {
                    val fleetName = result.data.fleet?.name
                    val status = result.data.status
                    _state.update {
                        it.copy(
                            isLoading = false,
                            success = true,
                            errorMessage = null,
                            queuedOffline = false,
                            fleetName = fleetName,
                            requestStatus = status
                        )
                    }
                    _refreshTrigger.tryEmit(Unit)
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            success = false,
                            queuedOffline = false
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    private fun queueInviteCodeRequest(inviteCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(REGISTRATION_INVITE_CODE, inviteCode).apply()
        enqueueImmediateUploadWork(context)
        scheduleDataUploadWork(context)
        _state.update {
            it.copy(
                isLoading = false,
                success = true,
                errorMessage = null,
                queuedOffline = true,
                requestStatus = "pending"
            )
        }
        _refreshTrigger.tryEmit(Unit)
    }

    private fun isNetworkError(message: String?): Boolean {
        return message?.contains("network", ignoreCase = true) == true
    }

    private fun isValidInviteCode(code: String): Boolean {
        val pattern = Regex("^[A-Z]{3,4}-[A-Z0-9]{5,8}$")
        return pattern.matches(code)
    }
}

data class JoinFleetUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
    val queuedOffline: Boolean = false,
    val fleetName: String? = null,
    val requestStatus: String? = null
)
