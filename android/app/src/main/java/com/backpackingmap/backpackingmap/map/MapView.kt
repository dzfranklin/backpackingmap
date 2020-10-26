package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.backpackingmap.backpackingmap.Coordinate
import com.backpackingmap.backpackingmap.asPixel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Supporting construction from just context & params would complicate the state without
// much benefit
@SuppressLint("ViewConstructor")
@OptIn(ExperimentalCoroutinesApi::class)
class MapView(
    context: Context,
    initialCenter: Coordinate,
    initialZoom: ZoomLevel,
    locationProcessor: ForegroundLocationProcessor,
) : View(context), CoroutineScope {
    // TODO: figure out when to cancel to avoid leaks
    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    private val gestureDetector = OmniGestureDetector(context)

    // Not applicable, as we just delegate to platform GestureDetectors
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(this, event)
    }

    private val initialState = MapState(
        center = initialCenter,
        zoom = initialZoom,
        MapSize(width.asPixel(), height.asPixel())
    )
    private val processor = MapProcessor(coroutineContext, initialState)

    init {
        // Process gestures
        gestureDetector.events
            .onEach {
                processor.send(MapProcessor.Event.Gesture(it))
            }
            .launchIn(this)
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        launch {
            processor.send(MapProcessor.Event.SizeChanged(MapSize(width.asPixel(), height.asPixel())))
        }
    }

    private val myPositionLayer = createLayer(MyPositionLayer.Builder(context, locationProcessor))

    private val layers = MutableStateFlow<List<MapLayer>>(emptyList())

    fun <T: MapLayer> setLayers(new: Collection<MapLayer.Builder<T>>) {
        layers.value = new.map(::createLayer) + myPositionLayer
    }

    private fun <T: MapLayer> createLayer(builder: MapLayer.Builder<T>) : T =
        // Create job so we can cancel layers separately
        builder.build(processor.state, ::postInvalidate, coroutineContext + Job())

    private val renderer = MapRenderer(coroutineContext, layers)

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        renderer.renderTo(canvas)
    }

    override fun onDetachedFromWindow() {
        for (layer in layers.value) {
            layer.onDetachedFromWindow()
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        for (layer in layers.value) {
            layer.onAttachedToWindow()
        }
        super.onAttachedToWindow()
    }
}