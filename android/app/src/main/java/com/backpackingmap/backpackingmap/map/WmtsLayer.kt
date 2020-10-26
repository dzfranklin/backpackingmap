package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.backpackingmap.backpackingmap.*
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
import org.locationtech.proj4j.units.Units
import kotlin.coroutines.CoroutineContext

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
    ) : MapLayer.Builder<WmtsLayer>() {
        override fun build(
            mapState: StateFlow<MapState>,
            requestRender: () -> Unit,
            coroutineContext: CoroutineContext,
        ) = WmtsLayer(context, config, repo, mapState, requestRender, coroutineContext)
    }

    override var render: RenderOperation = UnitRenderOperation

    private val requesting: MutableSet<GetTileRequest> = mutableSetOf()

    private sealed class Message {
        data class StateUpdate(val mapState: MapState) : Message()
        data class TileLoaded(val request: GetTileRequest) : Message()
    }

    private val actor = actor<Message> {
        for (msg in channel) {
            val state = when (msg) {
                is Message.StateUpdate -> msg.mapState
                is Message.TileLoaded -> {
                    requesting.remove(msg.request)
                    mapState.value
                }
            }
            render = computeRender(state)
            requestRender()
        }
    }

    init {
        launch {
            mapState.collect {
                actor.send(Message.StateUpdate(it))
            }
        }
    }

    private suspend fun onTileLoaded(request: GetTileRequest, bitmap: Bitmap) {
        actor.send(Message.TileLoaded(request))
    }

    /** Not coroutine-safe */
    private fun computeRender(mapState: MapState): RenderOperation {
        if (config.set.crs.projection.units != Units.METRES) {
            TODO("Support units other than meters")
        }

        // NOTE: The comments in WmtsTileMatrixSetConfig.tileIndices (called below) will help you
        // understand how this code works
        val (matrix, scaleFactor) = selectMatrix(mapState)

        val (_, centerXInCrs, centerYInCrs) = mapState.center.convertTo(config.set.crs)
        val centerX = Meter(centerXInCrs)
        val centerY = Meter(centerYInCrs)

        val pixelSpan = config.set.metersPerPixel(matrix)

        val effectiveHalfWidth = (mapState.size.width * pixelSpan) / (scaleFactor * 2.0)
        val effectiveHalfHeight = (mapState.size.height * pixelSpan) / (scaleFactor * 2.0)

        val minX = (centerX - effectiveHalfWidth).toDouble()
        val maxX = (centerX + effectiveHalfWidth).toDouble()
        val minY = (centerY - effectiveHalfHeight).toDouble()
        val maxY = (centerY + effectiveHalfHeight).toDouble()

        val tileRange = config.set.tileIndices(matrix,
            maxCrsX = maxX,
            minCrsX = minX,
            maxCrsY = maxY,
            minCrsY = minY
        )

        val tileWidth = matrix.tileWidth
        val tileHeight = matrix.tileHeight

        val overageX = (tileRange.minColOverage * tileWidth.toDouble()).asPixel()
        val overageY = (tileRange.minRowOverage * tileWidth.toDouble()).asPixel()

        val requestBuilder = GetTileRequest.Builder.from(config, matrix)

        val tiles: MutableList<RenderOperation> = mutableListOf()

        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = matrix.tileWidth * (col - tileRange.minColInclusive)
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY = matrix.tileHeight * (row - tileRange.minRowInclusive)

                val leftX = offsetX - overageX
                val topY = offsetY - overageY

                val request = requestBuilder.build(row, col)

                val cached = repo.getCached(request)
                val tile = if (cached != null) {
                    RenderBitmap(leftX, topY, cached)
                } else {
                    if (!requesting.contains(request)) {
                        requesting.add(request)
                        repo.requestCaching(request, ::onTileLoaded)
                    }
                    createRenderPlaceholder(leftX, topY, tileWidth, tileHeight)
                }

                tiles.add(tile)
            }
        }

        return if (!BuildConfig.RENDER_DEBUG_BOXES_AROUND_TILES) {
            RenderScaled(scaleFactor.toFloat(), RenderMultiple(tiles))
        } else {
            RenderMultiple(listOf(
                RenderScaled(scaleFactor.toFloat(),
                    RenderMultiple(tiles)),
            ) + extraDebugs(mapState, scaleFactor))
        }
    }

    private fun extraDebugs(mapState: MapState, scaleFactor: Double): List<RenderOperation> {
        val preScaleCenter = object : RenderOperation {
            override fun renderTo(canvas: Canvas) {
                canvas.drawCircle(
                    (mapState.size.width / (scaleFactor * 2.0)).toFloat(),
                    (mapState.size.height / (scaleFactor * 2.0)).toFloat(),
                    10f,
                    Paint().apply {
                        style = Paint.Style.FILL
                        color = Color.RED
                    }
                )
            }
        }

        val center = object : RenderOperation {
            override fun renderTo(canvas: Canvas) {
                canvas.drawCircle(
                    (mapState.size.width / 2.0).toFloat(),
                    (mapState.size.height / 2.0).toFloat(),
                    10f,
                    Paint().apply {
                        style = Paint.Style.FILL
                        color = Color.BLUE
                    }
                )
            }
        }

        val preScaleExtents = object : RenderOperation {
            override fun renderTo(canvas: Canvas) {
                canvas.drawRect(
                    0f,
                    0f,
                    mapState.size.width.toFloat() / scaleFactor.toFloat(),
                    mapState.size.height.toFloat() / scaleFactor.toFloat(),
                    Paint().apply {
                        style = Paint.Style.STROKE
                        color = Color.RED
                        strokeWidth = 5f
                    }
                )
            }
        }

        return listOf(preScaleCenter, center, preScaleExtents)
    }

    private fun selectMatrix(mapState: MapState): Pair<WmtsTileMatrixConfig, Double> {
        val (targetMetersPerPixel, matrixMetersPerPixel, matrix) =
            repo.findClosestMatrix(config, mapState.zoom)!!
        val scaleFactor = matrixMetersPerPixel / targetMetersPerPixel

        return matrix to scaleFactor
    }

    private data class RenderBitmap(
        private val leftX: Pixel,
        private val topY: Pixel,
        private val bitmap: Bitmap,
    ) : RenderOperation {
        override fun renderTo(canvas: Canvas) {
            canvas.drawBitmap(
                bitmap,
                leftX.toFloat(),
                topY.toFloat(),
                null
            )

            if (BuildConfig.RENDER_DEBUG_BOXES_AROUND_TILES) {
                canvas.drawRect(
                    leftX.toFloat(),
                    topY.toFloat(),
                    leftX.toFloat() + bitmap.width,
                    topY.toFloat() + bitmap.height,
                    debugBoxPaint!!
                )
            }
        }

        companion object {
            private val debugBoxPaint = if (BuildConfig.RENDER_DEBUG_BOXES_AROUND_TILES) {
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = Color.WHITE
                }
            } else {
                null
            }
        }
    }

    private fun createRenderPlaceholder(leftX: Pixel, topY: Pixel, width: Pixel, height: Pixel) =
        RenderPlaceholder(leftX, topY, width, height, placeholderPaint)

    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.unloadedTile)
    }

    private data class RenderPlaceholder(
        private val leftX: Pixel,
        private val topY: Pixel,
        private val width: Pixel,
        private val height: Pixel,
        private val placeholderPaint: Paint,
    ) : RenderOperation {
        override fun renderTo(canvas: Canvas) {
            canvas.drawRect(
                leftX.toFloat(),
                topY.toFloat(),
                (leftX + width).toFloat(),
                (topY + height).toFloat(),
                placeholderPaint
            )
        }
    }
}