package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
) :
    CoroutineScope {

    sealed class TouchEvent {
        data class Move(val deltaX: Float, val deltaY: Float) : TouchEvent()
        data class Scale(val factor: Float) : TouchEvent()
    }

    private val _events = MutableSharedFlow<TouchEvent>()
    val events = _events.asSharedFlow()

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

                // We invert because scrolling moves you in the opposite direction to the one your
                // finger literally moves in
                send(TouchEvent.Move(-1 * distanceX, -1 * distanceY))
                return true
            }
        })

    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (detector == null) {
                    return false
                }

                send(TouchEvent.Scale(1 / detector.scaleFactor))
                return true
            }
        })

    init {
        touchView.setOnTouchListener { _, event: MotionEvent ->
            gestureDetector.onTouchEvent(event) ||
                    scaleDetector.onTouchEvent(event) ||
                    touchView.onTouchEvent(event)
        }
    }

    private fun send(message: TouchEvent) {
        launch {
            _events.emit(message)
        }
    }
}