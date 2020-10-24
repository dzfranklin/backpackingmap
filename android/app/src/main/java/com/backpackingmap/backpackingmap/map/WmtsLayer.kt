package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.repo.TileRepo
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class WmtsLayer(context: Context, private val config: WmtsLayerConfig, private val repo: TileRepo) :
    MapLayer {
    override suspend fun computeRender(
        state: MapState,
        requestRerender: () -> Unit,
    ): Collection<RenderOperation> {
        // NOTE: The comments in WmtsTileMatrixSetConfig.tileIndices (called below) will help you
        // understand how this code works
        val (targetMetersPerPixel, matrixMetersPerPixel, matrix) =
            repo.findClosestMatrix(config, state.zoom)!!
        val metersPerPixelScaleFactor = targetMetersPerPixel / matrixMetersPerPixel
        val scaleFactor = 1 / metersPerPixelScaleFactor

        val (_, centerX, centerY) = state.center.convertTo(config.set.crs)
        val pixelSpan = config.set.metersPerPixel(matrix).toFloat()

        val effectiveWidth = state.size.width * pixelSpan / scaleFactor
        val effectiveHeight = state.size.height * pixelSpan / scaleFactor

        val minX = centerX - floor(effectiveWidth / 2.0)
        val maxX = centerX + ceil(effectiveWidth / 2.0)
        val minY = centerY - floor(effectiveHeight / 2.0)
        val maxY = centerY + ceil(effectiveHeight / 2.0)

        val tileRange = config.set.tileIndices(matrix,
            maxCrsX = maxX,
            minCrsX = minX,
            maxCrsY = maxY,
            minCrsY = minY
        )

        val overageX = round(tileRange.minColOverageInCrs / pixelSpan).toInt()
        val overageY = round(tileRange.minRowOverageInCrs / pixelSpan).toInt()

        val requestBuilder = GetTileRequest.Builder.from(config, matrix)

        val width = matrix.tileWidth.toFloat()
        val height = matrix.tileHeight.toFloat()

        val operations: MutableList<RenderOperation> = mutableListOf()

        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = (col - tileRange.minColInclusive).toFloat() * matrix.tileWidth.toFloat()
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY =
                    (row - tileRange.minRowInclusive).toFloat() * matrix.tileHeight.toFloat()

                val leftX = offsetX - overageX
                val topY = offsetY - overageY

                val request = requestBuilder.build(row, col)

                val cached = repo.getCached(request)
                val operation = if (cached != null) {
                    RenderBitmap(leftX, topY, cached)
                } else {
                    repo.requestCaching(request) {
                        requestRerender()
                    }
                    createRenderPlaceholder(leftX, topY, width, height)
                }

                operations.add(operation)
            }
        }

        return listOf(RenderScaled(scaleFactor, operations))
    }

    data class RenderBitmap(
        private val leftX: Float,
        private val topY: Float,
        private val bitmap: Bitmap,
    ) : RenderOperation {
        override fun renderTo(canvas: Canvas) {
            canvas.drawBitmap(
                bitmap,
                leftX,
                topY,
                null
            )
        }
    }

    private fun createRenderPlaceholder(leftX: Float, topY: Float, width: Float, height: Float) =
        RenderPlaceholder(leftX, topY, width, height, placeholderPaint)

    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.unloadedTile)
    }

    data class RenderPlaceholder(
        private val leftX: Float,
        private val topY: Float,
        private val width: Float,
        private val height: Float,
        private val placeholderPaint: Paint,
    ) : RenderOperation {
        override fun renderTo(canvas: Canvas) {
            canvas.drawRect(
                leftX,
                topY,
                leftX + width,
                topY + height,
                placeholderPaint
            )
        }
    }
}