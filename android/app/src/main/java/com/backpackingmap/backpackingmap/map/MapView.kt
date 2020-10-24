package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
        MapSize(width, height)
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
            processor.send(MapProcessor.Event.SizeChanged(MapSize(width, height)))
        }
    }

    private val layers = MutableStateFlow<List<MapLayer>>(emptyList())

    fun setLayers(new: Collection<MapLayer.Builder>) {
        layers.value = new.map(::createLayer)
    }

    private fun createLayer(builder: MapLayer.Builder) =
        // Create job so we can cancel layers separately
        builder.build(processor.state, coroutineContext + Job())

    private val renderer = MapRenderer(coroutineContext, layers)

    init {
        launch {
            renderer.operation.collect {
                postInvalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        renderer.operation.value.renderTo(canvas)
    }
}