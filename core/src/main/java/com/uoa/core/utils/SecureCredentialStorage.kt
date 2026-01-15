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
class SecureCredentialStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CREDENTIAL_PREFS_NAME = "secure_credential_prefs"
        private const val EMAIL_KEY = "driver_email"
        private const val PASSWORD_KEY = "driver_password"
        private const val TAG = "SecureCredentialStorage"
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
            context.deleteSharedPreferences(CREDENTIAL_PREFS_NAME)
            try {
                encryptedPrefsAvailable = true
                createEncryptedPrefs()
            } catch (second: Exception) {
                encryptedPrefsAvailable = false
                Log.e(TAG, "Encrypted prefs unavailable; credentials will not persist.", second)
                context.getSharedPreferences(CREDENTIAL_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            CREDENTIAL_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(email: String, password: String) {
        if (!encryptedPrefsAvailable) {
            Log.w(TAG, "Encrypted prefs unavailable; skipping credential save.")
            return
        }
        securePrefs.edit()
            .putString(EMAIL_KEY, email)
            .putString(PASSWORD_KEY, password)
            .apply()
    }

    fun getEmail(): String? {
        if (!encryptedPrefsAvailable) {
            return null
        }
        return securePrefs.getString(EMAIL_KEY, null)
    }

    fun getPassword(): String? {
        if (!encryptedPrefsAvailable) {
            return null
        }
        return securePrefs.getString(PASSWORD_KEY, null)
    }

    fun clearCredentials() {
        if (!encryptedPrefsAvailable) {
            return
        }
        securePrefs.edit()
            .remove(EMAIL_KEY)
            .remove(PASSWORD_KEY)
            .apply()
    }
}
