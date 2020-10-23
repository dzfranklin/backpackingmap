package com.backpackingmap.backpackingmap.map

data class MapState(
    val center: Coordinate,
    val zoom: ZoomLevel,
    val size: MapSize,
) {
    fun withCenter(newCenter: Coordinate) =
        MapState(center = newCenter, zoom = zoom, size = size)

    fun withZoom(newZoom: ZoomLevel) =
        MapState(center = center, zoom = newZoom, size = size)

    fun withSize(size: MapSize) =
        MapState(center = center, zoom = zoom, size = size)
}