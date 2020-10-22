package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
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
        val received: Long?,
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
                received = delta.received,
                zoom = prev.zoom.scaledBy(delta.zoomScaleFactor),
                center = prev.center.movedBy(metersEast, metersNorth),
            )

            if (delta.received != null) {
                Timber.i("Took ${System.currentTimeMillis() - delta.received} to emit")
            }

            _events.emit(next)
            prev = next
        }
    }

    private fun send(delta: Delta) {
        if (!processor.offer(delta)) {
            Timber.w("Processor refused delta")
        }
    }

    private var flinger: Job? = null

    private val gestureDetector = OmniGestureDetector(context) { event: OmniGestureDetector.Event, received: Long ->
        flinger?.cancel("Cancelling fling because of new motion event")

        when (event) {
            is OmniGestureDetector.Event.Scroll -> {
                send(Delta(
                    received = received,
                    zoomScaleFactor = 1f,
                    deltaX = event.distanceX,
                    deltaY = event.distanceY,
                ))
            }

            is OmniGestureDetector.Event.Fling -> {
                if (event.velocityX != null && event.velocityY != null) {
                    flinger = launch {
                        var deltaX = -event.velocityX / 15f
                        var deltaY = -event.velocityY / 15f

                        while (abs(deltaX) > 1 || abs(deltaY) > 1) {
                            send(Delta(
                                received = null,
                                zoomScaleFactor = 1f,
                                deltaX = deltaX,
                                deltaY = deltaY,
                            ))

                            deltaX *= 0.8f
                            deltaY *= 0.8f

                            delay(1_000 / 60) // 60 fps
                        }
                    }
                }
            }

            is OmniGestureDetector.Event.Scale -> {
                if (event.scaleFactor != null) {
                    send(Delta(
                        received = received,
                        zoomScaleFactor = 1 / event.scaleFactor,
                        deltaX = 0f,
                        deltaY = 0f,
                    ))
                }
            }
        }
    }

    init {
        touchView.setOnTouchListener(gestureDetector::onTouchEvent)
    }
}