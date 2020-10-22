package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
// Not applicable, as we just delegate to GestureDetector
@SuppressLint("ClickableViewAccessibility")
class GestureHandler(
    override val coroutineContext: CoroutineContext,
    context: Context,
    touchView: View,
    initialPosition: MapPosition,
) :
    CoroutineScope {

    private val _events =
        MutableSharedFlow<MapPosition>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    private data class Delta(
        val zoomScaleFactor: Float,
        val deltaX: Float,
        val deltaY: Float,
    )

    // NOTE: This actor ensures that an event is always based on the previous event, avoiding jumps
    // if two events both base themselves on the same event they think was the previous event.
    //
    // We can't shed because we work with deltas, so this should be relatively fast
    private val processor = actor<Delta> {
        var prev = initialPosition
        _events.emit(initialPosition)

        for (delta in channel) {
            // We invert because scrolling moves you in the opposite direction to the one your
            // finger literally moves in

            val metersNorth = -1 * delta.deltaY * prev.zoom.metersPerPixel

            // and then invert deltaX again (so not at all) because east is to the left
            val metersEast = delta.deltaX * prev.zoom.metersPerPixel

            val next = MapPosition(
                zoom = prev.zoom.scaledBy(delta.zoomScaleFactor),
                center = prev.center.movedBy(metersEast, metersNorth),
            )

            _events.emit(next)
            prev = next
        }
    }

    private fun send(delta: Delta) {
        if (!processor.offer(delta)) {
            Timber.w("Processor refused delta")
        }
    }

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

    private var flinger: Job? = null

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            var lastPointer: Int? = null

            override fun onScroll(
                initial: MotionEvent,
                current: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                // NOTE: distances since last call, not initial
                // See <https://developer.android.com/reference/android/view/GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent,%20android.view.MotionEvent,%20float,%20float)>

                val lastPointerCached = lastPointer
                if (lastPointerCached != null) {
                    if (current.findPointerIndex(lastPointerCached) != 0) {
                        // If the user switches pointers in the middle of a scroll we throw out the
                        // first event of the new pointer because its distanceX and distanceY are
                        // from the other pointer. If we used them the position would jump
                        lastPointer = current.getPointerId(0)
                        return true
                    }
                }
                lastPointer = current.getPointerId(0)

                send(Delta(
                    zoomScaleFactor = 1f,
                    deltaX = distanceX,
                    deltaY = distanceY,
                ))

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
                        send(Delta(
                            zoomScaleFactor = 1f,
                            deltaX = deltaX,
                            deltaY = deltaY,
                        ))

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

                val zoomScaleFactor = 1f / detector.scaleFactor

                send(Delta(
                    zoomScaleFactor = zoomScaleFactor,
                    deltaX = 0f,
                    deltaY = 0f,
                ))

                return true
            }
        })
}