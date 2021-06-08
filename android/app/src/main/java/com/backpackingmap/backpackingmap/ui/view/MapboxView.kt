package com.backpackingmap.backpackingmap.ui.view

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.distanceTo
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
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

                setOnTouchListener(state)
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


interface TouchArea {
    val id: Any

    val onPress: ((MotionEvent) -> Boolean)?
    val onLongPress: ((MotionEvent) -> Boolean)?
    val onDrag: OnDrag?
    val onDown: ((MotionEvent) -> Boolean)?
    val onUp: ((MotionEvent) -> Boolean)?

    fun contains(point: LatLng, map: MapboxMap, density: Density): Boolean

    data class OnDrag(
        val onStart: ((MotionEvent, LatLng) -> Boolean)? = null,
        val onDrag: ((MotionEvent, LatLng) -> Boolean)? = null,
        val onEnd: ((MotionEvent, LatLng) -> Boolean)? = null,
        val onCancel: (() -> Unit)? = null,
    )

    data class Entire(
        override val id: Any,
        override val onPress: ((MotionEvent) -> Boolean)? = null,
        override val onLongPress: ((MotionEvent) -> Boolean)? = null,
        override val onDrag: OnDrag? = null,
        override val onDown: ((MotionEvent) -> Boolean)? = null,
        override val onUp: ((MotionEvent) -> Boolean)? = null,
    ) : TouchArea {
        override fun contains(point: LatLng, map: MapboxMap, density: Density) = true
    }

    class Circle(
        override val id: Any,
        var center: LatLng,
        override val onPress: ((MotionEvent) -> Boolean)? = null,
        override val onLongPress: ((MotionEvent) -> Boolean)? = null,
        override val onDrag: OnDrag? = null,
        override val onDown: ((MotionEvent) -> Boolean)? = null,
        override val onUp: ((MotionEvent) -> Boolean)? = null,
        val radius: Dp = 15.dp,
    ) : TouchArea {
        override fun contains(point: LatLng, map: MapboxMap, density: Density): Boolean {
            val distMeters = center.distanceTo(point)
            val mapMetersPerPixel = map.projection.getMetersPerPixelAtLatitude(center.latitude)
            // meters * (1 / (meters/pixel)) = meters * (pixels / meter) = pixels
            val distPixels = distMeters / mapMetersPerPixel

            val radiusPixels = with(density) { radius.toPx() }

            return distPixels < radiusPixels
        }

        override fun hashCode(): Int =
            id.hashCode()

        override fun equals(other: Any?) =
            hashCode() == other.hashCode()
    }
}

@Composable
fun rememberMapboxState(
    initialStyle: String,
    initialPosition: CameraPosition? = null
): MapboxState {
    val coroutineContext = rememberCoroutineScope().coroutineContext
    val context = LocalContext.current
    val viewConfig = remember(context) { ViewConfiguration.get(context) }
    val touchSlop = remember(viewConfig) { viewConfig.scaledTouchSlop }
    val longPressTimeout = ViewConfiguration.getLongPressTimeout()
    val density = LocalDensity.current
    return remember(touchSlop, longPressTimeout, density, coroutineContext) {
        MapboxState(initialStyle, initialPosition, touchSlop, longPressTimeout, density, coroutineContext)
    }
}

class MapboxState(
    internal val initialStyle: String,
    internal val initialPosition: CameraPosition?,
    private val touchSlop: Int,
    private val longPressTimeout: Int,
    private val density: Density,
    override val coroutineContext: CoroutineContext
) :
    CoroutineScope, OnCameraTrackingChangedListener, View.OnTouchListener {

    private val _cameraPosition =
        MutableStateFlow(initialPosition ?: CameraPosition.Builder().build())

    /** Only updates on idle */
    val cameraPosition = _cameraPosition.asStateFlow()

    private val _cameraMode = MutableStateFlow(CameraMode.NONE)
    val cameraMode = _cameraMode.asStateFlow()

    private val _map = CompletableDeferred<MapboxMap>()
    private val _style = CompletableDeferred<Style>()

    private val _touchAreas = mutableListOf<TouchArea>()

    suspend fun awaitMap() =
        _map.await()

    suspend fun awaitStyle() =
        _style.await()

    /** Newer areas will go on top of older ones. */
    fun registerTouchArea(area: TouchArea) {
        _touchAreas.add(area)
    }

    fun deregisterTouchArea(id: Any) {
        for ((idx, area) in _touchAreas.withIndex()) {
            if (area.id == id) {
                _touchAreas.removeAt(idx)
            }
        }
    }

    /** Caller must ensure they have ACCESS_FINE_LOCATION permission */
    suspend fun trackLocation(context: Context, enable: Boolean) {
        val map = awaitMap()
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

    private var currentlyDown: CurrentDown? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (view == null || event == null) {
            Timber.d("Ignoring touch event as view or event null. view: %s, event: %s", view, event)
            return false
        }
        val cachedDown = currentlyDown

        if (event.actionMasked == MotionEvent.ACTION_CANCEL && cachedDown != null) {
            cachedDown.area.onDrag?.onCancel?.invoke()
            currentlyDown = null
        }

        if (!_map.isCompleted) {
            Timber.d("Ignoring touch event as map not loaded yet")
            return false
        }
        val map = _map.getCompleted()

        // NOTE: We reverse to obey contract of newer areas overriding
        val areas = _touchAreas.reversed()

        val dragHandlers = cachedDown?.area?.onDrag
        val onDragHandler = dragHandlers?.onDrag
        if (event.actionMasked == MotionEvent.ACTION_MOVE && onDragHandler != null) {
            val pointerIdx = event.findPointerIndex(cachedDown.pointerId)
            val screenPoint = PointF(event.getX(pointerIdx), event.getY(pointerIdx))
            val mapPoint = map.projection.fromScreenLocation(screenPoint)
            if (pointerIdx != -1 && cachedDown.isDrag(screenPoint, touchSlop)) {
                if (!cachedDown.dragStarted) {
                    dragHandlers.onStart?.invoke(event, mapPoint)
                    cachedDown.dragStarted = true
                }
                return onDragHandler(event, mapPoint)
            }
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val screenPoint = PointF(event.getX(0), event.getY(0))
            val mapPoint = map.projection.fromScreenLocation(screenPoint)

            for (area in areas) {
                if (!area.contains(mapPoint, map, density)) {
                    continue
                }

                val onDown = area.onDown
                if (onDown != null && onDown(event)) {
                    return true
                }

                val pointerId = event.getPointerId(0)
                currentlyDown = CurrentDown(area, screenPoint, mapPoint, pointerId)
            }
        }

        val actionIsUp = event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP
        val downPointerIdx = cachedDown?.let { event.findPointerIndex(it.pointerId) } ?: -1
        if (actionIsUp && cachedDown != null && downPointerIdx != -1) {
            for (area in areas) {
                if (cachedDown.area != area) {
                    continue
                }

                // If cachedDown.area matches this is the only area we check, so no continues
                // after here

                val screenPoint = PointF(event.getX(downPointerIdx), event.getY(downPointerIdx))
                val mapPoint = map.projection.fromScreenLocation(screenPoint)

                // If they don't handle onUp, proceed to the other handlers
                val onUp = cachedDown.area.onUp
                if (onUp != null && onUp(event)) {
                    return true
                }

                val isCaptured = when {
                    cachedDown.isDrag(screenPoint, touchSlop) -> {
                        onDragHandler != null && onDragHandler(event, mapPoint)
                    }
                    event.downTime < longPressTimeout -> {
                        val onPress = cachedDown.area.onPress
                        if (onPress != null && onPress(event)) {
                            view.performClick()
                            true
                        } else {
                            false
                        }
                    }
                    else -> {
                        val onLongPress = cachedDown.area.onLongPress
                        onLongPress != null && onLongPress(event)
                    }
                }

                currentlyDown = null
                if (isCaptured) {
                    return true
                }
            }
        }

        return false
    }
}

data class CurrentDown(
    val area: TouchArea,
    val startScreen: PointF,
    val startMap: LatLng,
    val pointerId: Int,
    var dragStarted: Boolean = false,
) {
    fun isDrag(latest: PointF, touchSlop: Int) =
        startScreen.distanceTo(latest) > touchSlop
}
