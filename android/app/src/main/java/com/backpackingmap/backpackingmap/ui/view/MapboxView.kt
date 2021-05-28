package com.backpackingmap.backpackingmap.ui.view

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.backpackingmap.backpackingmap.R
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext

@Composable
fun MapboxView(state: MapboxState, modifier: Modifier = Modifier) {
    val token = stringResource(R.string.public_token_mapbox)

    AndroidView(
        factory = { context ->
            Mapbox.getInstance(context, token)

            val options = MapboxMapOptions.createFromAttributes(context, null)
            if (state.initialPosition != null) {
                options.camera(state.initialPosition)
            }

            MapView(context, options).apply {
                getMapAsync {
                    it.setStyle(state.initialStyle)
                    state.registerMap(it)
                }
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
fun MapboxViewPreview() {
    val initialPosition = CameraPosition.Builder()
        .target(LatLng(43.7383, 7.4094))
        .zoom(5.0)
        .build()

    val state = rememberMapboxState(Style.OUTDOORS, initialPosition)
    LaunchedEffect(state) {
        val update = CameraUpdateFactory.zoomBy(10.0)
        state.awaitMap().animateCamera(update, 10_000)
    }

    MapboxView(state)
}

@Composable
fun rememberMapboxState(
    initialStyle: String,
    initialPosition: CameraPosition? = null
): MapboxState {
    val coroutineContext = rememberCoroutineScope().coroutineContext
    return remember {
        MapboxState(initialStyle, initialPosition, coroutineContext)
    }
}

class MapboxState(
    internal val initialStyle: String,
    internal val initialPosition: CameraPosition?,
    override val coroutineContext: CoroutineContext
) :
    CoroutineScope, OnCameraTrackingChangedListener {

    private val _cameraPosition =
        MutableStateFlow(initialPosition ?: CameraPosition.Builder().build())

    /** Only updates on idle */
    val cameraPosition = _cameraPosition.asStateFlow()

    private val _cameraMode = MutableStateFlow(CameraMode.NONE)
    val cameraMode = _cameraMode.asStateFlow()

    private val _map = CompletableDeferred<MapboxMap>()
    private val _style = CompletableDeferred<Style>()

    suspend fun awaitMap() =
        _map.await()

    suspend fun awaitStyle() =
        _style.await()

    /** Caller must ensure they have ACCESS_FINE_LOCATION permission */
    suspend fun trackLocation(context: Context, enable: Boolean) {
        val map = awaitMap()
        val style = awaitStyle()
        val loc = map.locationComponent

        if (enable) {
            if (!loc.isLocationComponentActivated) {
                showLocation(context)
            }

            loc.removeOnCameraTrackingChangedListener(this)
            loc.addOnCameraTrackingChangedListener(this)

            try {
                loc.isLocationComponentEnabled = true
            } catch (e: SecurityException) {
                throw IllegalStateException("Caller must ensure fine location permission")
            }
            loc.cameraMode = CameraMode.TRACKING_GPS
            loc.renderMode = RenderMode.GPS
        } else {
            if (loc.isLocationComponentActivated && loc.isLocationComponentEnabled) {
                loc.cameraMode = CameraMode.NONE
            }
        }
    }

    /** Caller must ensure they have ACCESS_FINE_LOCATION permission */
    suspend fun showLocation(context: Context) {
        val map = awaitMap()
        val style = awaitStyle()
        val loc = map.locationComponent

        val opts = LocationComponentOptions.builder(context)
            .pulseEnabled(true)
            .build()

        loc.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style)
                .locationComponentOptions(opts)
                .build()
        )

        try {
            loc.isLocationComponentEnabled = true
        } catch (e: SecurityException) {
            throw IllegalStateException("Caller must ensure fine location permission")
        }
        loc.cameraMode = CameraMode.NONE
        loc.renderMode = RenderMode.GPS
    }

    internal fun registerMap(map: MapboxMap) {
        if (!_map.complete(map)) {
            throw IllegalStateException("MapState already registered to a MapboxMap")
        }

        map.addOnCameraIdleListener {
            _cameraPosition.value = map.cameraPosition
        }

        map.getStyle {
            if (!_style.complete(it)) {
                throw IllegalStateException("Expected MapState style unset")
            }
        }
    }

    override fun onCameraTrackingChanged(currentMode: Int) {
        _cameraMode.value = currentMode
    }

    override fun onCameraTrackingDismissed() {
        // Note: Redundant, called after onCameraTrackingChanged
    }
}