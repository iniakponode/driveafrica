package com.uoa.sensor.presentation.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.uoa.core.model.Road
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapComposable(
    context: Context,
    latitude: Double,
    longitude: Double,
    roads: List<Road>,
    path: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    // Initialize OSMDroid configuration
    Configuration.getInstance().userAgentValue = context.packageName

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(it).apply {
                setMultiTouchControls(true)
                setBuiltInZoomControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(latitude, longitude))
            }
        },
        update = { map ->
            map.overlays.clear()

            // Show current user location
            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
            myLocationOverlay.enableMyLocation()
            map.overlays.add(myLocationOverlay)

            // Draw travelled path
            if (path.isNotEmpty()) {
                val polyline = Polyline().apply { setPoints(path) }
                map.overlays.add(polyline)
            }

            // Add markers for each road showing speed limits
            roads.forEach { road ->
                val marker = Marker(map).apply {
                    position = GeoPoint(road.latitude, road.longitude)
                    title = "Name: ${road.name}\nType: ${road.roadType}\nSpeed Limit: ${road.speedLimit} km/h"
                }
                map.overlays.add(marker)
            }

            map.controller.setCenter(GeoPoint(latitude, longitude))
            map.invalidate()
        }
    )
}
