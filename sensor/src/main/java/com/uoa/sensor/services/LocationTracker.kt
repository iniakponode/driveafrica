package com.uoa.sensor.services//package com.uoa.sensor.services
//
//import android.app.Service
//import android.content.Intent
//import android.location.Location
//import android.location.LocationListener
//import android.location.LocationManager
//import android.os.IBinder
//import com.uoa.sensor.data.database.model.SensorData
//
//class LocationService : Service(), LocationListener {
//    private lateinit var locationManager: LocationManager
//
//    override fun onCreate() {
//        super.onCreate()
//        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
//        try {
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10f, this)
//        } catch (e: SecurityException) {
//            // Handle exception
//        }
//    }
//
//    override fun onLocationChanged(location: Location) {
//        handleLocation(location)
//    }
//
//    private fun handleLocation(location: Location) {
//        if (location.hasSpeed()) {
//            val speedMps = location.speed
//            val sensorData = SensorData(
//                id = 0,
//                tripDataId = getCurrentTripId(),
//                timestamp = location.time,
//                synced = false,
//                speed = speedMps,
//            )
//            processSensorData(sensorData)
//        }
//    }
//
//    private fun processSensorData(sensorData: SensorData) {
//        // Add any required processing logic here
//        // For example, you might only want to store speeds above a certain threshold, etc.
//        RawSensorDataRepository(sensorDataDao).saveSensorData(sensorData)
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
////
//    // Other required methods for LocationListener and Service
//}
