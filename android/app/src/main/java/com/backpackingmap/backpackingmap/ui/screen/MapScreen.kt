package com.backpackingmap.backpackingmap.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.Destination
import com.backpackingmap.backpackingmap.ui.theme.LocalThemeExtras
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
fun MapScreen(nav: NavController, repo: Repo, ensureFineLocation: suspend () -> Boolean) {
    Scaffold(
        bottomBar = { BottomBar(Destination.Map) { nav.navigate(it.route) } }
    ) { contentPadding ->
        val mapbox = rememberMapboxState(Style.OUTDOORS)
        val context = LocalContext.current
        val isTracking = remember { mutableStateOf(false) }

        LaunchedEffect(mapbox) {
            val initialPosD = async { repo.mapPosition() }
            val mapD = async { mapbox.awaitMap() }
            val initialPos = initialPosD.await()
            val map = mapD.await()

            if (initialPos != null) {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(initialPos))
            }

            // Don't request just to show, but if we already have use
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        }

        LaunchedEffect(isTracking.value) {
            if (isTracking.value) {
                if (!ensureFineLocation()) {
                    isTracking.value = false
                    return@LaunchedEffect
                }
            }
            mapbox.trackLocation(context, isTracking.value)
        }

        ConstraintLayout(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val (map, locButton) = createRefs()

            MapboxView(mapbox, Modifier.constrainAs(map) {
                linkTo(parent.start, parent.top, parent.end, parent.bottom)
            })

            TrackingButton(
                isTracking.value,
                { isTracking.value = it },
                Modifier.constrainAs(locButton) {
                    bottom.linkTo(parent.bottom, margin = 20.dp)
                    end.linkTo(parent.end, margin = 20.dp)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                })
        }
    }
}

@Composable
fun TrackingButton(isTracking: Boolean, setIsTracking: (Boolean) -> Unit, modifier: Modifier) {
    Button(
        onClick = { setIsTracking(!isTracking) },
        shape = CircleShape,
        colors = buttonColors(MaterialTheme.colors.surface),
        contentPadding = PaddingValues(10.dp),
        modifier = modifier
    ) {
        val tint = if (isTracking) {
            LocalThemeExtras.current.isTrackingColor
        } else {
            MaterialTheme.colors.onSurface
        }

        Icon(
            painterResource(R.drawable.ic_track_location),
            stringResource(R.string.track_my_location),
            modifier = Modifier.size(30.dp),
            tint = tint,
        )
    }
}
