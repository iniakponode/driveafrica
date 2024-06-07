//package com.uoa.sensor.utils
//
//import android.Manifest
//import android.app.Activity
//import android.content.pm.PackageManager
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.viewmodel.compose.viewModel
//
//object PermissionUtils {
//
//    private const val LOCATION_PERMISSION_REQUEST_CODE = 1
//
//    fun requestLocationPermissions(activity: Activity) {
//        if (!hasLocationPermissions(activity)) {
//            ActivityCompat.requestPermissions(
//                activity,
//                arrayOf(
//                    android.Manifest.permission.ACCESS_FINE_LOCATION,
//                    android.Manifest.permission.ACCESS_COARSE_LOCATION
//                ),
//                LOCATION_PERMISSION_REQUEST_CODE
//            )
//        }
//    }
//
//    private fun hasLocationPermissions(activity: Activity): Boolean {
//        return ContextCompat.checkSelfPermission(
//            activity,
//            android.Manifest.permission.ACCESS_FINE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
//            activity,
//            android.Manifest.permission.ACCESS_COARSE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    fun handlePermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, onPermissionGranted: () -> Unit, onPermissionDenied: () -> Unit) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                onPermissionGranted()
//            } else {
//                onPermissionDenied()
//            }
//        }
//    }
//
//
//    // This function can be called from your Compose UI
//    fun requestPermissions(requestPermissionLauncher: ActivityResultLauncher<Array<String>>) {
//        val permissions = arrayOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.READ_CONTACTS
//        )
//        requestPermissionLauncher.launch(permissions)
//    }
//}
