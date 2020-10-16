package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTilePosition
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class MapLayer(
    context: Context,
    parent: ViewGroup,
    private val service: WmtsServiceConfig,
    private val config: WmtsLayerConfig,
    private val extents: MapExtents,
    private val repo: Repo,
) {
    private val surface = SurfaceView(context)
    private val holder: SurfaceHolder = surface.holder
    private lateinit var scope: CoroutineScope

    init {
        parent.addView(surface, parent.layoutParams)
        holder.setFixedSize(extents.screenWidth.toInt(), extents.screenHeight.toInt())

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Timber.i("Surface created")

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
                Timber.i("Surface destroyed")

                scope.cancel()
            }
        })
    }

    private val activeMatrix = config.matrices.keys.last()

    private suspend fun drawAllTiles() {
        val (_, centerX, centerY) = extents.center.convertTo(config.set.crs)
        val pixelSpan = config.set.pixelSpan(activeMatrix)
        val screenWidth = extents.screenWidth.toDouble() * pixelSpan
        val screenHeight = extents.screenHeight.toDouble() * pixelSpan

        val minX = centerX - floor(screenWidth / 2.0)
        val maxX = centerX + ceil(screenWidth / 2.0)
        val minY = centerY - floor(screenHeight / 2.0)
        val maxY = centerY + ceil(screenHeight / 2.0)

        val tileRange = config.set.tileIndices(activeMatrix,
            maxCrsX = maxX,
            minCrsX = minX,
            maxCrsY = maxY,
            minCrsY = minY
        )

        val overageX = round(tileRange.minColOverageInCrs / pixelSpan).toInt()
        val overageY = round(tileRange.minRowOverageInCrs / pixelSpan).toInt()

        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = (col - tileRange.minColInclusive) * activeMatrix.tileWidth.toInt()
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY = (row - tileRange.minRowInclusive) * activeMatrix.tileHeight.toInt()

                scope.launch {
                    val tile = repo.getTile(service,
                        config,
                        config.set,
                        activeMatrix,
                        WmtsTilePosition(col = col, row = row))
                        .map {
                            drawTile(it,
                                offsetX - overageX,
                                offsetY - overageY,
                                activeMatrix.tileWidth,
                                activeMatrix.tileHeight)
                        }
                        .mapLeft {
                            Timber.w("Error fetching tile: %s", it)
                        }
                }
            }
        }
    }

    private suspend fun drawTile(
        tile: Bitmap,
        leftX: Int,
        topY: Int,
        tileWidth: Pixel,
        tileHeight: Pixel,
    ) = withContext(Dispatchers.Main) {
        val canvas = holder.lockCanvas(Rect(
            // The X coordinate of the left side of the rectangle
            leftX,
            // The Y coordinate of the top of the rectangle
            topY,
            // The X coordinate of the right side of the rectangle
            leftX + tileWidth.toInt(),
            // The Y coordinate of the bottom of the rectangle
            topY + tileHeight.toInt()
        ))

        canvas.drawBitmap(tile, leftX.toFloat(), topY.toFloat(), null)

        holder.unlockCanvasAndPost(canvas)
    }
}
