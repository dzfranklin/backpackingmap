package com.backpackingmap.backpackingmap.ui.screen

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.Destination
import com.backpackingmap.backpackingmap.ui.view.BottomBar
import com.backpackingmap.backpackingmap.ui.view.MapboxView
import com.backpackingmap.backpackingmap.ui.view.rememberMapboxState
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@Composable
fun MapScreen(nav: NavController, repo: Repo, ensureFineLocation: suspend () -> Boolean) {
    Scaffold(
        bottomBar = { BottomBar(Destination.Map) { nav.navigate(it.route)} }
    ) {
        val mapbox = rememberMapboxState(Style.OUTDOORS)
        val context = LocalContext.current

        val scope = rememberCoroutineScope()
        scope.launch {
            val initialPos = repo.mapPosition()
            val map = mapbox.awaitMap()

            if (initialPos != null) {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(initialPos))
            }

            val locOpts = LocationComponentActivationOptions.Builder(context, mapbox.awaitStyle()).build();
            map.locationComponent.activateLocationComponent(locOpts)

            map.addOnCameraIdleListener {
                scope.launch {
                    repo.setMapPosition(map.cameraPosition)
                }
            }
        }

        MapboxView(mapbox)
    }
}