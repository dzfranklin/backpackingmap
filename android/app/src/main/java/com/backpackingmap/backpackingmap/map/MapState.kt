package com.backpackingmap.backpackingmap.map

import com.backpackingmap.backpackingmap.Coordinate

data class MapState(
    val center: Coordinate,
    val zoom: ZoomLevel,
    val size: MapSize,
)
