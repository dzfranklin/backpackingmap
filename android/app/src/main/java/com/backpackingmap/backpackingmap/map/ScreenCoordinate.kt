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

data class Foo(val a: Int, val b: Int, val c: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Foo

        if (a != other.a) return false
        if (b != other.b) return false
        if (c != other.c) return false

        return true
    }

    override fun hashCode(): Int {
        var result = a
        result = 31 * result + b
        result = 31 * result + c
        return result
    }
}