package com.uoa.sensor.presentation.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.uoa.core.model.Road
import kotlinx.coroutines.flow.MutableStateFlow
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
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember { MapView(context) }
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
            enableCompass()
        }
    }
    val pathOverlay = remember { Polyline() }

    val userLocationFlow = remember { MutableStateFlow(GeoPoint(latitude, longitude)) }
    LaunchedEffect(latitude, longitude) {
        userLocationFlow.value = GeoPoint(latitude, longitude)
    }
    val currentLocation by userLocationFlow.collectAsState()

    Box(modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = {
                mapView.apply {
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(true)
                    controller.setZoom(15.0)
                    if (!overlays.contains(myLocationOverlay)) overlays.add(myLocationOverlay)
                    if (!overlays.contains(pathOverlay)) overlays.add(pathOverlay)
                }
            },
            update = { map ->
                pathOverlay.setPoints(path)
                map.overlays.removeAll { it is Marker }
                roads.forEach { road ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(road.latitude, road.longitude)
                        title = "Name: ${road.name}\nType: ${road.roadType}\nSpeed Limit: ${road.speedLimit} km/h"
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        )

        FloatingActionButton(
            onClick = { mapView.controller.animateTo(currentLocation) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Recenter")
        }
    }
}
