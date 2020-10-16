package com.backpackingmap.backpackingmap.map

data class MapExtents(
    val screenWidth: Pixel,
    val screenHeight: Pixel,
    val center: Coordinate,
    val zoom: ZoomLevel,
)
