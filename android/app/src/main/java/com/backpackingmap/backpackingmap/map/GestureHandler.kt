package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

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

    init {
        launch {
            send(initialPosition)
        }
    }

    private var lastPosition = initialPosition

    private var minimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private var maximumFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private var flinger: Job? = null

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

                val newPosition = computeCenterMovedBy(lastPosition, distanceX, distanceY)
                lastPosition = newPosition

                launch {
                    send(newPosition)
                }

                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                flinger = launch {
                    var deltaX = -velocityX / 15f
                    var deltaY = -velocityY / 15f

                    while (abs(deltaX) > 1 || abs(deltaY) > 1) {
                        val newPosition =
                            computeCenterMovedBy(lastPosition, deltaX, deltaY)
                        lastPosition = newPosition
                        send(newPosition)

                        deltaX *= 0.8f
                        deltaY *= 0.8f

                        delay(1_000 / 60) // 60 fps
                    }
                }

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

                launch {
                    send(newPosition)
                }

                return true
            }
        })

    init {
        touchView.setOnTouchListener { _, event: MotionEvent ->
            flinger?.cancel("Cancelling fling because of new motion event")

            if (event.pointerCount > 1) {
                scaleDetector.onTouchEvent(event)
            } else {
                gestureDetector.onTouchEvent(event)
            }

            true
        }
    }

    private fun normalizeFlingVelocity(velocity: Float) =
        (velocity - minimumFlingVelocity) / maximumFlingVelocity

    private fun computeCenterMovedBy(
        lastPosition: MapPosition,
        distanceX: Float,
        distanceY: Float,
    ): MapPosition {
        // We invert because scrolling moves you in the opposite direction to the one your
        // finger literally moves in

        val metersNorth = -1 * distanceY * lastPosition.zoom.metersPerPixel

        // and then invert distanceX again because east is to the left
        val metersEast = distanceX * lastPosition.zoom.metersPerPixel

        return MapPosition(
            zoom = lastPosition.zoom,
            center = lastPosition.center.movedBy(metersEast, metersNorth)
        )
    }

    private suspend fun send(newPosition: MapPosition) {
        _events.emit(newPosition)
    }
}