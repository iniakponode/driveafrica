package com.uoa.core.apiServices.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents = _sessionExpiredEvents.asSharedFlow()

    fun notifySessionExpired() {
        _sessionExpiredEvents.tryEmit(Unit)
    }
}
