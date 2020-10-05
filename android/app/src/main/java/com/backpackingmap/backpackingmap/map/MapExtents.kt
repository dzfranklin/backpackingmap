package com.backpackingmap.backpackingmap.map

data class MapExtents(
    val xPixels: Int,
    val yPixels: Int,
    val center: LatLng,
    val zoom: ZoomLevel,
    val surfaceWidthMultiple: Int = 5,
    val surfaceHeightMultiple: Int = 5
) {
    fun pixelOffsetToLatLng(offsetX: Int, offsetY: Int): LatLng {

    }

    fun surfaceWidth() = yPixels * surfaceWidthMultiple

    fun surfaceHeight() = xPixels * surfaceHeightMultiple
}
