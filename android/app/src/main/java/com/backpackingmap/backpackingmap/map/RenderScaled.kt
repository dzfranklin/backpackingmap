package com.backpackingmap.backpackingmap.map

import android.graphics.Canvas
import androidx.core.graphics.withScale

data class RenderScaled(
    val scaleFactor: Float,
    val operation: RenderOperation,
) : RenderOperation {
    override fun renderTo(canvas: Canvas) {
        canvas.withScale(scaleFactor, scaleFactor) {
            operation.renderTo(canvas)
        }
    }
}