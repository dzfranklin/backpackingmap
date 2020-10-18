package com.backpackingmap.backpackingmap.repo

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.backpackingmap.backpackingmap.map.wmts.*

class TileCache {
    private val appMaxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val size = appMaxMemory / 4

    private data class TileIdentifier(
        val service: WmtsServiceConfig,
        val layer: WmtsLayerConfig,
        val set: WmtsTileMatrixSetConfig,
        val matrix: WmtsTileMatrixConfig,
        val position: WmtsTilePosition,
    )

    private val cache = object : LruCache<TileIdentifier, Bitmap>(size) {
        override fun sizeOf(key: TileIdentifier, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun insert(
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        set: WmtsTileMatrixSetConfig,
        matrix: WmtsTileMatrixConfig,
        position: WmtsTilePosition,
        bitmap: Bitmap,
    ) {
        cache.put(TileIdentifier(service, layer, set, matrix, position), bitmap)
    }

    fun get(
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        set: WmtsTileMatrixSetConfig,
        matrix: WmtsTileMatrixConfig,
        position: WmtsTilePosition,
    ): Bitmap? =
        cache.get(TileIdentifier(service, layer, set, matrix, position))
}