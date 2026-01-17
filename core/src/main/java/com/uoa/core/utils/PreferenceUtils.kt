package com.uoa.core.utils

import android.content.Context
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.ALLOW_METERED_UPLOADS
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.TRIP_ID
import com.uoa.core.utils.Constants.Companion.REGISTRATION_PENDING
import com.uoa.core.utils.Constants.Companion.REGISTRATION_COMPLETED
import java.util.UUID

object PreferenceUtils {
     // Replace with your actual key

    fun getDriverProfileId(context: Context): UUID? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null) ?: return null
        return try {
            UUID.fromString(profileIdString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun getTripId(context:Context): UUID?{
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tripString = prefs.getString(TRIP_ID, null) ?: return null
        return try {
            UUID.fromString(tripString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun isMeteredUploadsAllowed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(ALLOW_METERED_UPLOADS, true)
    }

    fun setMeteredUploadsAllowed(context: Context, allowed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(ALLOW_METERED_UPLOADS, allowed).apply()
    }

    fun setRegistrationPending(context: Context, pending: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(REGISTRATION_PENDING, pending).apply()
    }

    fun isRegistrationPending(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(REGISTRATION_PENDING, false)
    }

    fun setRegistrationCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(REGISTRATION_COMPLETED, completed).apply()
    }

    fun isRegistrationCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(REGISTRATION_COMPLETED, false)
    }
}
