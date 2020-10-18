package com.backpackingmap.backpackingmap.map

import android.graphics.Rect

data class TilePlacement(
    val topLeftX: Int,
    val topLeftY: Int,
    val width: Pixel,
    val height: Pixel,
) {
    fun toRect() =
        Rect(
            topLeftX,
            topLeftY,
            topLeftX + width.toInt(),
            topLeftY + height.toInt()
        )
}