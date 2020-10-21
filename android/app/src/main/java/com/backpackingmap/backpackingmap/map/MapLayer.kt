package com.backpackingmap.backpackingmap.map.layer

import android.content.Context
import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.graphics.withTranslation
import arrow.core.Either
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.MapPosition
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.repo.GetTileError
import com.backpackingmap.backpackingmap.repo.Repo
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.properties.Delegates

class MapLayer constructor(context: Context) : View(context) {
    val repo = Repo.fromContext(context)!!.tileRepo

    private lateinit var config: WmtsLayerConfig
    private lateinit var position: MapPosition
    private var attached = false

    fun onAttachToMap(newConfig: WmtsLayerConfig, newPosition: MapPosition) {
        config = newConfig
        position = newPosition
        attached = true
    }

    private var screenHeight by Delegates.notNull<Int>()
    private var screenWidth by Delegates.notNull<Int>()
    private var sized = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        sized = true
        computeTiles()
    }

    fun onChangePosition(newPosition: MapPosition) {
        position = newPosition
        computeTiles()
    }

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

    private var tiles: List<Tile> = listOf()
    private var scaleFactor = 1f

    private fun computeTiles() {
        if (!attached || !sized) {
            return
        }

        // NOTE: This is in case they change in the middle of a call
        val cachedConfig = config
        val cachedScreenWidth = screenWidth
        val cachedScreenHeight = screenHeight
        val cachedPosition = position

        val (targetMetersPerPixel, matrixMetersPerPixel, matrix) = repo.findClosestMatrix(
            cachedConfig,
            cachedPosition)!!
        val metersPerPixelScaleFactor = targetMetersPerPixel / matrixMetersPerPixel
        val newScaleFactor = 1 / metersPerPixelScaleFactor

        val (_, centerX, centerY) = cachedPosition.center.convertTo(cachedConfig.set.crs)
        val pixelSpan = cachedConfig.set.metersPerPixel(matrix).toFloat()

        val effectiveWidth = cachedScreenWidth * pixelSpan / newScaleFactor
        val effectiveHeight = cachedScreenHeight * pixelSpan / newScaleFactor

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

        val size = (tileRange.maxColInclusive - tileRange.minColInclusive) *
                (tileRange.maxRowInclusive - tileRange.minRowInclusive)

        val toRequest: MutableList<GetTileRequest> = mutableListOf()
        val newTiles: MutableList<Tile> = mutableListOf()

        var i = 0
        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = (col - tileRange.minColInclusive).toFloat() * matrix.tileWidth.toFloat()
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY =
                    (row - tileRange.minRowInclusive).toFloat() * matrix.tileHeight.toFloat()
                i++

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

        repo.requestCaching(toRequest) {
            computeTiles()
        }

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null || !attached || !sized) {
            return
        }

        val cachedScaleFactor = scaleFactor
        val cachedTiles = tiles

        canvas.scale(cachedScaleFactor, cachedScaleFactor)

        canvas.drawRect(0f, 0f, 100f, 100f, placeholderPaint)

        for (tile in cachedTiles) {
            when (tile.value) {
                is TileType.Raster ->
                    drawTile(canvas, tile.value.bitmap, tile.leftX, tile.topY)

                is TileType.Error ->
                    drawError(canvas,
                        tile.value.error,
                        tile.leftX,
                        tile.topY,
                        tile.width,
                        tile.height)

                is TileType.Placeholder ->
                    drawPlaceholder(canvas, tile.leftX, tile.topY, tile.width, tile.height)
            }
        }
    }

    private val density = resources.displayMetrics.density

    private val tileDebugBoxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.DKGRAY
        strokeWidth = 2f * density
    }

    private fun drawTile(canvas: Canvas, bitmap: Bitmap, topLeftX: Float, topLeftY: Float) {
        canvas.drawBitmap(
            bitmap,
            topLeftX,
            topLeftY,
            null
        )

        if (DRAW_DEBUG_TILE_BOXES) {
            canvas.drawRect(
                topLeftX,
                topLeftY,
                topLeftX + bitmap.width,
                topLeftY + bitmap.height,
                tileDebugBoxPaint
            )
        }
    }

    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.unloadedTile)
    }

    private val errorPaint = TextPaint().apply {
        color = context.getColor(R.color.tileErrorText)
        textSize = 20F * density
    }

    private val errorPadding = 5F * density

    private fun drawPlaceholder(
        canvas: Canvas,
        topLeftX: Float,
        topLeftY: Float,
        width: Float,
        height: Float,
    ) {
        canvas.drawRect(
            topLeftX,
            topLeftY,
            topLeftX + width,
            topLeftY + height,
            placeholderPaint
        )

        if (DRAW_DEBUG_TILE_BOXES) {
            canvas.drawRect(
                topLeftX,
                topLeftY,
                topLeftX + width,
                topLeftY + height,
                tileDebugBoxPaint
            )
        }
    }

    private fun drawError(
        canvas: Canvas,
        error: GetTileError,
        topLeftX: Float,
        topLeftY: Float,
        width: Float,
        height: Float,
    ) {
        val text = error.toString()

        val axisPadding = 2 * errorPadding

        val internalWidth = (width - axisPadding).toInt()

        val rect = Rect()
        errorPaint.getTextBounds(text, 0, text.length, rect)
        val textLineHeight = rect.height()
        val internalHeightInLines =
            floor((height - axisPadding) / textLineHeight).toInt()

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, errorPaint, internalWidth)
            .setMaxLines(internalHeightInLines)
            .build()

        canvas.withTranslation(topLeftX + errorPadding, topLeftY + errorPadding) {
            layout.draw(this)
        }
    }

    companion object {
        private const val DRAW_DEBUG_TILE_BOXES = false
    }
}
