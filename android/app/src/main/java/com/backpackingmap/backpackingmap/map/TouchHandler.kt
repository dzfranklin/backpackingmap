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

    private var last: ScreenCoordinate? = null

    init {
        touchView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    last?.let { currentLast ->
                        val now = ScreenCoordinate.fromEvent(event)
                        val delta = now - currentLast
                        last = now
                        launch {
                            _events.emit(TouchEvent.Move(delta))
                        }
                    }
                }

                MotionEvent.ACTION_DOWN -> {
                    last = ScreenCoordinate.fromEvent(event)
                    // required for accessibility
                    view.performClick()
                }

                MotionEvent.ACTION_UP ->
                    last = null
            }

            // return value is whether the event was handled
            true
        }
    }
}