package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.repo.GetTileError
import com.backpackingmap.backpackingmap.repo.TileRepo
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class MapLayer constructor(context: Context) : View(context) {
    data class Attrs(
        val service: WmtsServiceConfig,
        val config: WmtsLayerConfig,
        val size: MapSize,
        val initialPosition: MapPosition,
        val repo: TileRepo,
    )

    var attrs: Attrs? = null
    var position: MapPosition? = null

    fun onReceiveAttrs(newAttrs: Attrs) {
        attrs = newAttrs
        invalidate()
    }

    fun onChangePosition(newPosition: MapPosition) {
        position = newPosition
        invalidate()
    }

    private val requested = HashSet<Int>()

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            Timber.w("onDraw with null canvas")
            return
        }

        val cachedAttrs = attrs ?: return
        val service = cachedAttrs.service
        val config = cachedAttrs.config
        val size = cachedAttrs.size
        val repo = cachedAttrs.repo

        val position = position ?: cachedAttrs.initialPosition

        val activeMatrix = config.matrices.keys.last() // TODO: Set based on zoom
        val serviceLayerMatrixIdentifier =
            repo.serviceLayerMatrixIdentifier(service, config, activeMatrix)

        val (_, centerX, centerY) = position.center.convertTo(config.set.crs)
        val pixelSpan = config.set.pixelSpan(activeMatrix)
        val screenWidth = size.screenWidth.toDouble() * pixelSpan
        val screenHeight = size.screenHeight.toDouble() * pixelSpan

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
                val offsetY =
                    (row - tileRange.minRowInclusive) * activeMatrix.tileHeight.toInt()

                val topLeftX = offsetX - overageX
                val topLeftY = offsetY - overageY
                val width = activeMatrix.tileWidth
                val height = activeMatrix.tileHeight

                val cached = repo.getCached(serviceLayerMatrixIdentifier, row, col)
                if (cached != null) {
                    drawTile(canvas, cached, topLeftX, topLeftY)
                } else {
                    drawPlaceholder(canvas, topLeftX, topLeftY, width, height)

                    val identifier = repo.tileIdentifier(serviceLayerMatrixIdentifier, row, col)
                    if (!requested.contains(identifier)) {
                        repo.requestCaching(service, config, activeMatrix, row, col) {
                            invalidate()
                        }
                        Timber.i("Requesting tile")
                        requested.add(identifier)
                    }
                }
            }
        }
    }

    private fun drawTile(canvas: Canvas, bitmap: Bitmap, topLeftX: Int, topLeftY: Int) {
        canvas.drawBitmap(
            bitmap,
            topLeftX.toFloat(),
            topLeftY.toFloat(),
            null
        )
    }

    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.unloadedTile)
    }

    private val errorPaint = Paint().apply {
        color = Color.RED
    }

    private fun drawPlaceholder(
        canvas: Canvas,
        topLeftX: Int,
        topLeftY: Int,
        width: Pixel,
        height: Pixel,
    ) {


        canvas.drawRect(
            topLeftX.toFloat(),
            topLeftY.toFloat(),
            (topLeftX + width.toInt()).toFloat(),
            (topLeftY + height.toInt()).toFloat(),
            placeholderPaint
        )
    }

    private fun drawError(canvas: Canvas, error: GetTileError, topLeftX: Int, topLeftY: Int) {
        canvas.drawText(error.toString(), topLeftX.toFloat(), topLeftY.toFloat(), errorPaint)
    }
}

// TODO: use
class TileNoLongerNeeded() : CancellationException("Tile no longer needed")
