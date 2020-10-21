package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
// Not applicable, as we just delegate to GestureDetector
@SuppressLint("ClickableViewAccessibility")
class GestureHandler(
    override val coroutineContext: CoroutineContext,
    context: Context,
    private val touchView: View,
    initialPosition: MapPosition,
) :
    CoroutineScope {

    private val _events =
        MutableSharedFlow<MapPosition>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    init { send(initialPosition) }

    private var lastPosition = initialPosition

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                initial: MotionEvent,
                current: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                // NOTE: distances since last call, not initial
                // See <https://developer.android.com/reference/android/view/GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent,%20android.view.MotionEvent,%20float,%20float)>

                val lastCached = lastPosition
                val zoom = lastCached.zoom

                // We invert because scrolling moves you in the opposite direction to the one your
                // finger literally moves in

                val metersNorth = -1 * distanceY * zoom.metersPerPixel

                // and then invert distanceX again because east is to the left
                val metersEast = distanceX * zoom.metersPerPixel

                val newPosition = MapPosition(
                    zoom = zoom,
                    center = lastCached.center.movedBy(metersEast, metersNorth)
                )
                lastPosition = newPosition
                send(newPosition)

                return true
            }
        })

    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (detector == null) {
                    return false
                }

                // TODO: Cap max and min scale
                val lastCached = lastPosition
                val newPosition = MapPosition(
                    center = lastCached.center,
                    zoom = lastCached.zoom.scaledBy(1 / detector.scaleFactor)
                )
                lastPosition = newPosition
                send(newPosition)

                return true
            }
        })

    init {
        touchView.setOnTouchListener { _, event: MotionEvent ->
            if (event.pointerCount > 1) {
                scaleDetector.onTouchEvent(event)
            } else {
                gestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    private fun send(newPosition: MapPosition) {
        launch {
            _events.emit(newPosition)
        }
    }
}