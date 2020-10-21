package com.backpackingmap.backpackingmap.map

data class ZoomLevel(val metersPerPixel: Float) {
    fun scaledBy(factor: Float) = ZoomLevel(metersPerPixel * factor)
}

