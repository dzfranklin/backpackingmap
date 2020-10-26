package com.backpackingmap.backpackingmap.map

import com.backpackingmap.backpackingmap.MetersPerPixel

data class ZoomLevel(val level: MetersPerPixel) {
    // TODO: Cap max and min scale
    fun scaledBy(factor: Double) = ZoomLevel(level * factor)
}

