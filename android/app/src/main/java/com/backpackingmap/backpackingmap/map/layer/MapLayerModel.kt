package com.backpackingmap.backpackingmap.map.layer

import android.graphics.Bitmap
import arrow.core.Either
import com.backpackingmap.backpackingmap.map.MapPosition
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.repo.GetTileError
import com.backpackingmap.backpackingmap.repo.TileRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

@OptIn(ExperimentalCoroutinesApi::class)
class MapLayerModel(
    private val repo: TileRepo,
    private val onShouldRedraw: () -> Unit,
) : CoroutineScope {

    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    data class Tile(
        val leftX: Float,
        val topY: Float,
        val width: Float,
        val height: Float,
        val value: TileType,
    )

    sealed class TileType {
        data class Raster(val bitmap: Bitmap) : TileType()
        object Placeholder : TileType()
        data class Error(val error: GetTileError) : TileType()
    }

    var tiles: List<Tile> = listOf()
    var scaleFactor = 1f

    data class ScreenSize(val width: Float, val height: Float)

    private var screenSize: ScreenSize? = null
    private var config: WmtsLayerConfig? = null
    private var position: SharedFlow<MapPosition>? = null

    fun onSizeChanged(newWidth: Int, newHeight: Int) {
        val newScreenSize = ScreenSize(newWidth.toFloat(), newHeight.toFloat())
        screenSize = newScreenSize

        val cachedPosition = position
        if (config != null && cachedPosition != null) {
            attachToPosition(cachedPosition)
        }
    }

    fun onAttachToMap(newConfig: WmtsLayerConfig, newPosition: SharedFlow<MapPosition>) {
        config = newConfig
        position = newPosition

        val cachedScreenSize = screenSize
        if (cachedScreenSize != null) {
            attachToPosition(newPosition)
        }
    }

    private var attached = false
    private var currentPosition: MapPosition? = null

    private fun attachToPosition(position: SharedFlow<MapPosition>) {
        if (attached) {
            throw IllegalStateException("Already attached to a position")
        }

        launch {
            var begun = 0
            var finished = 0
            position.collectLatest {
                begun++
                currentPosition = it
                computeTiles()
                finished++
                if (begun - finished > 0) {
                    Timber.i("Cancelled: %s", begun - finished)
                }
            }
        }
    }

    private fun computeTiles() {
        val cachedConfig = config
        val cachedPosition = currentPosition
        val cachedScreenSize = screenSize

        if (cachedConfig == null || cachedPosition == null || cachedScreenSize == null) {
            return
        }

        // NOTE: The comments in WmtsTileMatrixSetConfig.tileIndices (called below) will help you
        // understand how this code works

        val (targetMetersPerPixel, matrixMetersPerPixel, matrix) =
            repo.findClosestMatrix(cachedConfig, cachedPosition)!!
        val metersPerPixelScaleFactor = targetMetersPerPixel / matrixMetersPerPixel
        val newScaleFactor = 1 / metersPerPixelScaleFactor

        val (_, centerX, centerY) = cachedPosition.center.convertTo(cachedConfig.set.crs)
        val pixelSpan = cachedConfig.set.metersPerPixel(matrix).toFloat()

        val effectiveWidth = cachedScreenSize.width * pixelSpan / newScaleFactor
        val effectiveHeight = cachedScreenSize.height * pixelSpan / newScaleFactor

        val minX = centerX - floor(effectiveWidth / 2.0)
        val maxX = centerX + ceil(effectiveWidth / 2.0)
        val minY = centerY - floor(effectiveHeight / 2.0)
        val maxY = centerY + ceil(effectiveHeight / 2.0)

        val tileRange = cachedConfig.set.tileIndices(matrix,
            maxCrsX = maxX,
            minCrsX = minX,
            maxCrsY = maxY,
            minCrsY = minY
        )

        val overageX = round(tileRange.minColOverageInCrs / pixelSpan).toInt()
        val overageY = round(tileRange.minRowOverageInCrs / pixelSpan).toInt()

        val requestBuilder = GetTileRequest.Builder.from(cachedConfig, matrix)


        val width = matrix.tileWidth.toFloat()
        val height = matrix.tileHeight.toFloat()

        val toRequest: MutableList<GetTileRequest> = mutableListOf()
        val newTiles: MutableList<Tile> = mutableListOf()

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
                    when (cached) {
                        is Either.Left ->
                            Tile(leftX, topY, width, height, TileType.Error(cached.a))
                        is Either.Right ->
                            Tile(leftX, topY, width, height, TileType.Raster(cached.b))
                    }
                } else {
                    toRequest.add(request)
                    Tile(leftX, topY, width, height, TileType.Placeholder)
                }

                newTiles.add(tile)
            }
        }

        tiles = newTiles
        scaleFactor = newScaleFactor

        val cachedReceived = cachedPosition.received
        if (cachedReceived != null) {
            Timber.i("Built tiles for event in ${System.currentTimeMillis() - cachedReceived}")
            cachedPosition.received = null
        }

        onShouldRedraw()

        repo.requestCaching(toRequest) {
            computeTiles()
            onShouldRedraw()
        }
    }
}