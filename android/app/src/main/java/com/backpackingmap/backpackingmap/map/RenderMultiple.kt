package com.backpackingmap.backpackingmap.map

import android.graphics.Canvas

class RenderMultiple(private val operations: Collection<RenderOperation>) : RenderOperation {
    override fun renderTo(canvas: Canvas) {
        for (operation in operations) {
            operation.renderTo(canvas)
        }
    }
}