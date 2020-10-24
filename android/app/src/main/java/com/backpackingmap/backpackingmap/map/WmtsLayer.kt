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
import kotlinx.coroutines.flow.*
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
    override val coroutineContext: CoroutineContext,
) : MapLayer(), CoroutineScope {
    data class Builder(
        private val context: Context,
        private val config: WmtsLayerConfig,
        private val repo: TileRepo,
    ): MapLayer.Builder() {
        override fun build(mapState: StateFlow<MapState>, coroutineContext: CoroutineContext) =
            WmtsLayer(context, config, repo, mapState, coroutineContext)
    }

    data class State(
        val mapState: MapState,
        val tiles: Map<GetTileRequest, TileState>,
        val requesting: Set<GetTileRequest>,
    ) {
        constructor(mapState: MapState) : this(mapState, HashMap(), HashSet())
    }

    sealed class TileState {
        data class Loading(val leftX: Float, val topY: Float) : TileState()
        data class Loaded(val leftX: Float, val topY: Float, val bitmap: Bitmap) : TileState()
    }

    private val state = MutableStateFlow(State(mapState.value))

    private sealed class Message {
        data class MapStateUpdate(val mapState: MapState) : Message()
        data class TileLoaded(val request: GetTileRequest, val bitmap: Bitmap) : Message()
    }

    private val actor = actor<Message> {
        for (msg in channel) {
            when (msg) {
                is Message.MapStateUpdate -> {
                    val cached = state.value
                    val newTiles = cached.tiles.toMutableMap()
                    val newRequesting = cached.requesting.toMutableSet()

                    for ((request, tileState) in getCoveredTiles(msg.mapState)) {
                        newTiles[request] = tileState

                        if (tileState is TileState.Loading && !newRequesting.contains(request)) {
                            newRequesting.add(request)
                            repo.requestCaching(request, ::onTileLoaded)
                        }
                    }

                    state.value = State(msg.mapState, newTiles.toMap(), cached.requesting)
                }

                is Message.TileLoaded -> {
                    val cached = state.value

                    val newTiles = cached.tiles.toMutableMap()
                    val prevValue = newTiles[msg.request]
                    if (prevValue != null && prevValue is TileState.Loading) {
                        newTiles[msg.request] =
                            TileState.Loaded(prevValue.leftX, prevValue.topY, msg.bitmap)

                        val newRequesting = cached.requesting.toMutableSet()
                        newRequesting.remove(msg.request)
                        state.value = State(cached.mapState, newTiles, newRequesting)
                    }
                }
            }
        }
    }

    init {
        launch {
            mapState.collect {
                actor.send(Message.MapStateUpdate(it))
            }
        }
    }

    private suspend fun onTileLoaded(request: GetTileRequest, bitmap: Bitmap) {
        actor.send(Message.TileLoaded(request, bitmap))
    }

    override val render = state
        .transform<State, RenderOperation> { state ->
            val (matrix, scaleFactor) = selectMatrix(state.mapState)
            val width = matrix.tileWidth.toFloat()
            val height = matrix.tileHeight.toFloat()

            val operations = state.tiles.values
                .map { tile ->
                    when (tile) {
                        is TileState.Loading ->
                            createRenderPlaceholder(tile.leftX, tile.topY, width, height)
                        is TileState.Loaded ->
                            RenderBitmap(tile.leftX, tile.topY, tile.bitmap)
                    }
                }

            RenderScaled(scaleFactor, RenderMultiple(operations))
        }
        .stateIn(this, SharingStarted.Eagerly, UnitRenderOperation)


    private fun getCoveredTiles(mapState: MapState): Map<GetTileRequest, TileState> {
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

        val overageX = round(tileRange.minColOverageInCrs / pixelSpan).toInt()
        val overageY = round(tileRange.minRowOverageInCrs / pixelSpan).toInt()

        val requestBuilder = GetTileRequest.Builder.from(config, matrix)

        val tiles = HashMap<GetTileRequest, TileState>()

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
                    TileState.Loaded(leftX, topY, cached)
                } else {
                    TileState.Loading(leftX, topY)
                }

                tiles[request] = tile
            }
        }

        return tiles
    }

    private fun selectMatrix(mapState: MapState): Pair<WmtsTileMatrixConfig, Float> {
        val (targetMetersPerPixel, matrixMetersPerPixel, matrix) =
            repo.findClosestMatrix(config, mapState.zoom)!!
        val metersPerPixelScaleFactor = targetMetersPerPixel / matrixMetersPerPixel
        val scaleFactor = 1 / metersPerPixelScaleFactor

        return matrix to scaleFactor
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