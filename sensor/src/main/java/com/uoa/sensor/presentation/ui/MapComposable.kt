package com.uoa.sensor.presentation.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import com.uoa.core.model.Road
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapComposable(context: Context, latitude: Double, longitude: Double, roads: List<Road>) {
    // Initialize OSMDroid configuration
    Configuration.getInstance().userAgentValue = context.packageName

    AndroidView(factory = {
        MapView(it).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(latitude, longitude))

            // Add markers for each road
            for (road in roads) {
                val marker = Marker(this)
                marker.position = GeoPoint(road.latitude, road.longitude)
                marker.title = "Name: ${road.name}\nType: ${road.roadType}\nSpeed Limit: ${road.speedLimit} km/h"
                overlays.add(marker)
            }
        }
    })
}
