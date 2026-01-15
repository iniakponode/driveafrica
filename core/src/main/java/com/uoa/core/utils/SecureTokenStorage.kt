package com.uoa.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TOKEN_PREFS_NAME = "secure_token_prefs"
        private const val TOKEN_KEY = "jwt_token"
        private const val TAG = "SecureTokenStorage"
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private var encryptedPrefsAvailable = true

    private val securePrefs by lazy {
        createSecurePrefs()
    }

    private fun createSecurePrefs(): SharedPreferences {
        return try {
            encryptedPrefsAvailable = true
            createEncryptedPrefs()
        } catch (exception: Exception) {
            Log.e(TAG, "Encrypted prefs failed to load, resetting storage.", exception)
            context.deleteSharedPreferences(TOKEN_PREFS_NAME)
            try {
                encryptedPrefsAvailable = true
                createEncryptedPrefs()
            } catch (second: Exception) {
                encryptedPrefsAvailable = false
                Log.e(TAG, "Encrypted prefs unavailable; tokens will not persist.", second)
                context.getSharedPreferences(TOKEN_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            TOKEN_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        if (!encryptedPrefsAvailable) {
            Log.w(TAG, "Encrypted prefs unavailable; skipping token save.")
            return
        }
        securePrefs.edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    fun getToken(): String? {
        if (!encryptedPrefsAvailable) {
            return null
        }
        return securePrefs.getString(TOKEN_KEY, null)
    }

    fun clearToken() {
        if (!encryptedPrefsAvailable) {
            return
        }
        securePrefs.edit()
            .remove(TOKEN_KEY)
            .apply()
    }
}
