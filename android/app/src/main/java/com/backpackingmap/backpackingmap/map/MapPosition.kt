package com.backpackingmap.backpackingmap.map

data class MapPosition(
    var received: Long? = null,
    val center: Coordinate,
    val zoom: ZoomLevel,
)