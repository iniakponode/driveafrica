package com.uoa.core.apiServices.interceptor

import android.util.Log
import com.uoa.core.apiServices.session.SessionManager
import com.uoa.core.utils.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TokenExpiredInterceptor"

@Singleton
class TokenExpiredInterceptor @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            Log.w(TAG, "Clearing stored JWT after receiving 401 Unauthorized")
            secureTokenStorage.clearToken()
            sessionManager.notifySessionExpired()
        }
        return response
    }
}
