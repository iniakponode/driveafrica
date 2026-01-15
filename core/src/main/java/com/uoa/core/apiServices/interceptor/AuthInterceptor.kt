package com.uoa.core.apiServices.interceptor

import com.uoa.core.utils.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = secureTokenStorage.getToken()
        val requestBuilder = chain.request().newBuilder()
        token?.takeIf { it.isNotBlank() }
            ?.let { bearer -> requestBuilder.addHeader("Authorization", "Bearer $bearer") }
        return chain.proceed(requestBuilder.build())
    }
}
