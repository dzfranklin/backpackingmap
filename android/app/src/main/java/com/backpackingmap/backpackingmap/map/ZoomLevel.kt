package com.backpackingmap.backpackingmap.map

data class ZoomLevel(val metersPerPixel: Float) {
    // TODO: Cap max and min scale
    fun scaledBy(factor: Float) = ZoomLevel(metersPerPixel * factor)
}

