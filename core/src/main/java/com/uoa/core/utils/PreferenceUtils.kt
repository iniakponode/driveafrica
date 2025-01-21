package com.uoa.core.utils

import android.content.Context
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.TRIP_ID
import java.util.UUID

object PreferenceUtils {
     // Replace with your actual key

    fun getDriverProfileId(context: Context): UUID? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
        return profileIdString?.let { UUID.fromString(it) }
    }

    fun getTripId(context:Context): UUID?{
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tripString = prefs.getString(TRIP_ID, null)
        return tripString?.let { UUID.fromString(it) }
    }
}