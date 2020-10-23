package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// Supporting construction from just context & params would complicate the state without
// much benefit
@SuppressLint("ViewConstructor")
@OptIn(ExperimentalCoroutinesApi::class)
class MapView(
    context: Context,
    layers: Collection<MapLayer>,
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
    private val processor = MapProcessor(coroutineContext, initialState, layers)

    init {
        // Process gestures
        gestureDetector.events
            .onEach {
                processor.send(MapProcessor.Event.Gesture(it))
            }
            .launchIn(this)
    }

    private val renderer = MapRenderer(coroutineContext, processor.state, layers)

    init {
        // Redraw on new render
        launch {
            renderer.operations.collect {
                postInvalidateOnAnimation()
            }
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        launch {
            processor.send(MapProcessor.Event.SizeChanged(MapSize(width, height)))
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        for ((_, operations) in renderer.operations.value) {
            for (operation in operations) {
                operation.renderTo(canvas)
            }
        }
    }
}