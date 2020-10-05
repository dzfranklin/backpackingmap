package com.backpackingmap.backpackingmap.map

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.SurfaceHolder
import kotlinx.coroutines.*

class MapLayer(
    private val holder: SurfaceHolder,
    private val config: MapLayerConfig,
    private val extents: MapExtents,
) {
    lateinit var scope: CoroutineScope

    init {
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    drawAllTiles()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                scope.cancel()
            }
        })
    }


    private suspend fun drawAllTiles() {
        val tileSize = config.tileSize(extents.zoom)

        for (topY in 0..extents.yPixels step tileSize.pixelsY) {
            for (leftX in 0..extents.xPixels step tileSize.pixelsX) {
                val latLng = extents.pixelOffsetToLatLng(leftX, topY)
                val tile = config.getTile(latLng, extents.zoom)
                drawTile(tile, leftX, topY, tileSize)
            }
        }
    }

    private suspend fun drawTile(
        tile: Bitmap,
        leftX: Int,
        topY: Int,
        tileSize: TileSize,
    ) = withContext(Dispatchers.Main) {
        val canvas = holder.lockCanvas(Rect(
            // The X coordinate of the left side of the rectangle
            leftX,
            // The Y coordinate of the top of the rectangle
            topY,
            // The X coordinate of the right side of the rectangle
            leftX + tileSize.pixelsX,
            // The Y coordinate of the bottom of the rectangle
            topY + tileSize.pixelsY
        ))

        canvas.drawBitmap(tile, 0f, 0f, null)

        holder.unlockCanvasAndPost(canvas)
    }
}