package com.uoa.sensor.presentation.ui

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.uoa.core.model.Road
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    modifier: Modifier = Modifier,
    mapHeight: Dp = 300.dp,                 // fixed height container
    onStopMonitoring: () -> Unit = {}       // stop button action
) {
    Configuration.getInstance().userAgentValue = context.packageName

    // Remember MapView once and force MATCH_PARENT params
    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    // Overlays remembered once
    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
    }
    val pathOverlay = remember(mapView) { Polyline() }

    // Track current location to recenter with the FAB
    val userLocationFlow = remember { MutableStateFlow(GeoPoint(latitude, longitude)) }
    LaunchedEffect(latitude, longitude) {
        userLocationFlow.emit(GeoPoint(latitude, longitude))
    }
    var currentLocation by remember { mutableStateOf(GeoPoint(latitude, longitude)) }
    LaunchedEffect(Unit) {
        userLocationFlow.collectLatest { currentLocation = it }
    }

    // Update overlays when inputs change
    LaunchedEffect(path) {
        pathOverlay.setPoints(path)
        if (!mapView.overlays.contains(pathOverlay) && path.isNotEmpty()) {
            mapView.overlays.add(pathOverlay)
        } else if (path.isEmpty()) {
            mapView.overlays.remove(pathOverlay)
        }
        mapView.invalidate()
    }
    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(myLocationOverlay)) {
            mapView.overlays.add(myLocationOverlay)
        }
        mapView.invalidate()
    }
    LaunchedEffect(latitude, longitude) {
        mapView.controller.setCenter(GeoPoint(latitude, longitude))
    }

    // --- UI container: fixed-height Box keeps buttons on-screen over the map ---
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)     // fixed height
            .clipToBounds()
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),     // fill the Box; MapView has MATCH_PARENT LPs
            factory = { mapView },
            update = { map ->
                // Markers for roads
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

        // Recenter FAB (stays visible while interacting with the map)
        FloatingActionButton(
            onClick = { mapView.controller.animateTo(currentLocation) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Filled.MyLocation, contentDescription = "Recenter")
        }

        // Stop Monitoring button (also stays visible)
//        Button(
//            onClick = onStopMonitoring,
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(16.dp)
//        ) {
//            Text("Stop Monitoring")
//        }
    }
}

