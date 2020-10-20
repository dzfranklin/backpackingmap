package com.backpackingmap.backpackingmap.map

data class ZoomLevel(val metersPerPixel: Double) {
    fun scaledBy(factor: Float) = ZoomLevel(metersPerPixel * factor)
}

