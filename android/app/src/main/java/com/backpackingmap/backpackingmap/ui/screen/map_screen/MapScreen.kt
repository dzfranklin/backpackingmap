package com.backpackingmap.backpackingmap.ui.screen.map_screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.Destination
import com.backpackingmap.backpackingmap.ui.view.BottomBar
import com.backpackingmap.backpackingmap.ui.view.MapboxView
import com.backpackingmap.backpackingmap.ui.view.rememberMapboxState
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    repo: Repo,
    navigateTo: (Destination) -> Unit,
    ensureFineLocation: suspend () -> Boolean
) {
    val context = LocalContext.current
    val mapbox = rememberMapboxState(Style.OUTDOORS)
    val primaryState = remember { mutableStateOf(PrimaryState.Main) }
    val isTracking = remember { mutableStateOf(false) }

    LaunchedEffect(mapbox) {
        val initialPosD = async { repo.mapPosition() }
        val mapD = async { mapbox.awaitMap() }
        val initialPos = initialPosD.await()
        val map = mapD.await()

        if (initialPos != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(initialPos))
        }

        if (grantedLocation(context)) {
            mapbox.showLocation(context)
        }

        launch {
            mapbox.cameraPosition.collect {
                repo.setMapPosition(map.cameraPosition)
            }
        }

        launch {
            mapbox.cameraMode.collect {
                if (it != CameraMode.TRACKING_GPS && isTracking.value) {
                    isTracking.value = false
                }
            }
        }

        launch {
            snapshotFlow { isTracking.value }
                .collect {
                    if (it) {
                        if (!ensureFineLocation()) {
                            isTracking.value = false
                            return@collect
                        }
                    }
                    mapbox.trackLocation(context, isTracking.value)
                }
        }
    }

    Scaffold(
        bottomBar = { BottomBar(Destination.Map, navigateTo) }
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            MapboxView(mapbox, Modifier.fillMaxSize())

            when (primaryState.value) {
                PrimaryState.Main ->
                    MapMain(
                        isTracking.value,
                        setIsTracking = { isTracking.value = it },
                        onSelectCreate = {
                            when (it) {
                                CreateOption.Point -> primaryState.value = PrimaryState.CreateMarker
                                CreateOption.Route -> primaryState.value = PrimaryState.CreateRoute
                            }
                        }
                    )
                PrimaryState.CreateMarker ->
                    MapCreateMarker(repo, mapbox)
                PrimaryState.CreateRoute -> {
                    MapCreateRoute(repo, mapbox)
                }
            }
        }
    }
}

enum class PrimaryState {
    Main,
    CreateMarker,
    CreateRoute,
}

fun grantedLocation(context: Context) =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
