package com.backpackingmap.backpackingmap.map

import android.view.MotionEvent

data class ScreenCoordinate(val x: Float, val y: Float) {
    data class Delta(val x: Float, val y: Float)

    operator fun minus(other: ScreenCoordinate) =
        Delta(x - other.x, y - other.y)

    companion object {
        fun fromEvent(event: MotionEvent) =
            ScreenCoordinate(event.x, event.y)
    }
}
