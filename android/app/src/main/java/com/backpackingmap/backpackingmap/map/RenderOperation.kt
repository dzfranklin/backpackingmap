package com.backpackingmap.backpackingmap.map

import android.graphics.Canvas

interface RenderOperation {
    fun renderTo(canvas: Canvas)
}