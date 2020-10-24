package com.backpackingmap.backpackingmap.map

import android.graphics.Canvas

interface RenderOperation {
    fun renderTo(canvas: Canvas)

    operator fun plus(other: RenderOperation): RenderOperation {
        return RenderMultiple(listOf(this, other))
    }
}