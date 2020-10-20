package com.backpackingmap.backpackingmap.map

import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class TouchHandler(override val coroutineContext: CoroutineContext, private val touchView: View) :
    CoroutineScope {

    sealed class TouchEvent {
        data class Move(val delta: ScreenCoordinate.Delta) : TouchEvent()
    }

    private val _events = MutableSharedFlow<TouchEvent>()
    val events = _events.asSharedFlow()

    // Int represents pointer id
    private var dragLast: Pair<Int, ScreenCoordinate>? = null

    init {
        touchView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    dragLast?.let { (lastPointerId, lastCoordinate) ->
                        ScreenCoordinate.fromEvent(event, lastPointerId)?.let { now ->
                            val delta = now - lastCoordinate
                            dragLast = lastPointerId to now

                            launch {
                                _events.emit(TouchEvent.Move(delta))
                            }
                        }
                    }
                }

                MotionEvent.ACTION_DOWN -> {
                    val pointerId = event.getPointerId(FIRST_POINTER)
                    ScreenCoordinate.fromEvent(event, pointerId)?.let {
                        dragLast = pointerId to it
                    }
                    // required for accessibility
                    view.performClick()
                }

                MotionEvent.ACTION_UP ->
                    dragLast = null
            }

            // return value is whether the event was handled
            true
        }
    }

    companion object {
        private const val FIRST_POINTER = 0
    }
}