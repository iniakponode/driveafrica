package com.uoa.sensor.presentation.ui

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.uoa.core.model.Road
import com.uoa.sensor.R
import com.uoa.sensor.utils.displaySpeedLimitKmh
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

@Composable
fun MapComposable(
    context: Context,
    latitude: Double,
    longitude: Double,
    roads: List<Road>,
    currentRoadName: String?,
    speedLimitKmh: Int,
    path: List<GeoPoint>,
    isVehicleMoving: Boolean,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 320.dp,                 // fixed height container
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
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    // Overlays remembered once
    val rotationOverlay = remember(mapView) {
        RotationGestureOverlay(mapView).apply { isEnabled = true }
    }
    val pathOverlay = remember(mapView) { Polyline() }
    val vehicleMarker = remember(mapView) {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_vehicle_marker)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
        }
    }

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
        if (!mapView.overlays.contains(rotationOverlay)) {
            mapView.overlays.add(rotationOverlay)
        }
        if (!mapView.overlays.contains(vehicleMarker)) {
            mapView.overlays.add(vehicleMarker)
        }
        mapView.invalidate()
    }
    LaunchedEffect(latitude, longitude) {
        mapView.controller.setCenter(GeoPoint(latitude, longitude))
    }
    LaunchedEffect(latitude, longitude, path) {
        val currentPoint = GeoPoint(latitude, longitude)
        vehicleMarker.position = currentPoint
        vehicleMarker.rotation = 0f
        mapView.invalidate()
    }
    LaunchedEffect(isVehicleMoving) {
        val markerIcon = if (isVehicleMoving) {
            R.drawable.ic_vehicle_marker
        } else {
            R.drawable.ic_human_marker
        }
        vehicleMarker.icon = ContextCompat.getDrawable(context, markerIcon)
        mapView.invalidate()
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
                map.overlays.add(vehicleMarker)
                roads.forEach { road ->
                    val displaySpeedLimit = displaySpeedLimitKmh(context, road.speedLimit)
                    val marker = Marker(map).apply {
                        position = GeoPoint(road.latitude, road.longitude)
                        title = "Name: ${road.name}\nType: ${road.roadType}\nSpeed Limit: ${displaySpeedLimit} km/h"
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        )

        val roadLabel = currentRoadName?.takeIf { it.isNotBlank() } ?: "Unknown road"
        val speedLabel = if (speedLimitKmh > 0) "$speedLimitKmh km/h" else "--"
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 12.dp, end = 12.dp, top = 64.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "Road: $roadLabel",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Speed limit: $speedLabel",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

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

