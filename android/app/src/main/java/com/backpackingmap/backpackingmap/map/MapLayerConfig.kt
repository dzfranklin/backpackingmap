package com.backpackingmap.backpackingmap.map

import android.graphics.Bitmap

interface MapLayerConfig {
    fun tileSize(zoomLevel: ZoomLevel): TileSize

    suspend fun getTile(latLng: LatLng, zoomLevel: ZoomLevel): Bitmap
}
