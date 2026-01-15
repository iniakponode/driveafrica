package com.uoa.sensor.services

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.uoa.core.notifications.VehicleNotificationManager

class BootReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.w("BootReceiver", "Skipping auto-start on Android 12+; user must open app")
                VehicleNotificationManager(context).displayPermissionNotification(
                    "Open Safe Drive Africa to enable monitoring"
                )
                return
            }
            if (!hasLocationPermission(context)) {
                Log.w("BootReceiver", "Missing location permission; skipping auto-start")
                VehicleNotificationManager(context).displayPermissionNotification(
                    "Grant location permission to enable monitoring"
                )
                return
            }
            Intent(context, VehicleMovementServiceUpdate::class.java).also { serviceIntent ->
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
