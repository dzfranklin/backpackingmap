package com.backpackingmap.backpackingmap.map

import android.graphics.Canvas
import androidx.core.graphics.withScale

data class RenderScaled(
    val scaleFactor: Float,
    val operations: Collection<RenderOperation>,
) : RenderOperation {
    override fun renderTo(canvas: Canvas) {
        canvas.withScale(scaleFactor, scaleFactor) {
            for (operation in operations) {
                operation.renderTo(canvas)
            }
        }
    }
}