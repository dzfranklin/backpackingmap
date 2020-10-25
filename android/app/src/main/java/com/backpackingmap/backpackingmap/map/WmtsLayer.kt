package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTileMatrixConfig
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.repo.TileRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
class WmtsLayer constructor(
    context: Context,
    private val config: WmtsLayerConfig,
    private val repo: TileRepo,
    mapState: StateFlow<MapState>,
    private val requestRender: () -> Unit,
    override val coroutineContext: CoroutineContext,
) : MapLayer(), CoroutineScope {
    data class Builder(
        private val context: Context,
        private val config: WmtsLayerConfig,
        private val repo: TileRepo,
    ) : MapLayer.Builder() {
        override fun build(
            mapState: StateFlow<MapState>,
            requestRender: () -> Unit,
            coroutineContext: CoroutineContext,
        ) =
            WmtsLayer(context, config, repo, mapState, requestRender, coroutineContext)
    }

    override var render: RenderOperation = UnitRenderOperation

    private val requesting: MutableSet<GetTileRequest> = mutableSetOf()

    private val actor = actor<Unit> {
        for (msg in channel) {
            render = computeRender(mapState.value)
            requestRender()
        }
    }

    init {
        launch {
            mapState.collect {
                actor.send(Unit)
            }
        }
    }

    private suspend fun onTileLoaded(request: GetTileRequest, bitmap: Bitmap) {
        requesting.remove(request)
        actor.send(Unit)
    }

    /** Not coroutine-safe */
    private fun computeRender(mapState: MapState): RenderOperation {
        // NOTE: The comments in WmtsTileMatrixSetConfig.tileIndices (called below) will help you
        // understand how this code works
        val (matrix, scaleFactor) = selectMatrix(mapState)

        val (_, centerX, centerY) = mapState.center.convertTo(config.set.crs)
        val pixelSpan = config.set.metersPerPixel(matrix).toFloat()

        val effectiveWidth = mapState.size.width * pixelSpan / scaleFactor
        val effectiveHeight = mapState.size.height * pixelSpan / scaleFactor

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

        val width = matrix.tileWidth.toFloat()
        val height = matrix.tileHeight.toFloat()

        val overageX = round(tileRange.minColOverageInCrs / pixelSpan).toInt()
        val overageY = round(tileRange.minRowOverageInCrs / pixelSpan).toInt()

        val requestBuilder = GetTileRequest.Builder.from(config, matrix)

        val tiles: MutableList<RenderOperation> = mutableListOf()

        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = (col - tileRange.minColInclusive).toFloat() * matrix.tileWidth.toFloat()
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY =
                    (row - tileRange.minRowInclusive).toFloat() * matrix.tileHeight.toFloat()

                val leftX = offsetX - overageX
                val topY = offsetY - overageY

                val request = requestBuilder.build(row, col)

                val cached = repo.getCached(request)
                val tile = if (cached != null) {
                    RenderBitmap(leftX, topY, cached)
                } else {
                    if (!requesting.contains(request)) {
                        repo.requestCaching(request, ::onTileLoaded)
                    }
                    createRenderPlaceholder(leftX, topY, width, height)
                }

                tiles.add(tile)
            }
        }

        return RenderScaled(scaleFactor, RenderMultiple(tiles))
    }

    private fun selectMatrix(mapState: MapState): Pair<WmtsTileMatrixConfig, Float> {
        val (targetMetersPerPixel, matrixMetersPerPixel, matrix) =
            repo.findClosestMatrix(config, mapState.zoom)!!
        val metersPerPixelScaleFactor = targetMetersPerPixel / matrixMetersPerPixel
        val scaleFactor = 1 / metersPerPixelScaleFactor

        return matrix to scaleFactor
    }

    private data class RenderBitmap(
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

    private data class RenderPlaceholder(
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