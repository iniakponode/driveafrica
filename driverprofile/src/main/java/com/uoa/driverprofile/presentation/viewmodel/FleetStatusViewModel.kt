package com.uoa.driverprofile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.apiServices.services.fleetApiService.DriverFleetApiRepository
import com.uoa.core.network.NetworkMonitor
import com.uoa.core.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FleetStatusViewModel @Inject constructor(
    private val driverFleetApiRepository: DriverFleetApiRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(FleetStatusUiState())
    val state = _state.asStateFlow()
    private var pollingJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    fun refreshFleetStatus() {
        viewModelScope.launch {
            val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
            if (!online) {
                _state.update { it.copy(isLoading = false, errorMessage = null) }
                return@launch
            }
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = driverFleetApiRepository.getFleetStatus()) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        fleetStatus = result.data,
                        errorMessage = null
                    )
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) {
            return
        }
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refreshFleetStatus()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun cancelJoinRequest() {
        viewModelScope.launch {
            val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
            if (!online) {
                _state.update { it.copy(isLoading = false, errorMessage = "No internet connection.") }
                return@launch
            }
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = driverFleetApiRepository.cancelPendingRequest()) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, errorMessage = null) }
                    refreshFleetStatus()
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5000L
    }
}

data class FleetStatusUiState(
    val isLoading: Boolean = false,
    val fleetStatus: FleetStatusResponse? = null,
    val errorMessage: String? = null
)
