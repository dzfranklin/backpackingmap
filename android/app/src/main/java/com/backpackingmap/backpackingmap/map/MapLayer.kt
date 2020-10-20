package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.graphics.withTranslation
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTileMatrixConfig
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.repo.GetTileError
import com.backpackingmap.backpackingmap.repo.TileRepo
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class MapLayer constructor(context: Context) : View(context) {
    data class Attrs(
        val service: WmtsServiceConfig,
        val config: WmtsLayerConfig,
        val initialPosition: MapPosition,
        val repo: TileRepo,
    )

    var attrs: Attrs? = null

    private var position: MapPosition? = null
    private var screenHeight: Int? = null
    private var screenWidth: Int? = null

    fun onReceiveAttrs(newAttrs: Attrs) {
        attrs = newAttrs
        invalidateData()
    }

    fun onChangePosition(newPosition: MapPosition) {
        position = newPosition
        invalidateData()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        invalidateData()
    }

    private data class TileToDraw(
        val topLeftX: Float,
        val topLeftY: Float,
        val width: Float,
        val height: Float,
        val request: GetTileRequest,
    )

    private data class ToDraw(
        val tiles: Array<TileToDraw>,
        val scaleFactor: Float
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ToDraw

            if (!tiles.contentEquals(other.tiles)) return false
            if (scaleFactor != other.scaleFactor) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tiles.contentHashCode()
            result = 31 * result + scaleFactor.hashCode()
            return result
        }
    }

    private var toDraw: ToDraw? = null

    private fun invalidateData() {
        val cachedScreenWidth = screenWidth
        val cachedScreenHeight = screenHeight

        if (cachedScreenWidth == null || cachedScreenHeight == null) {
            return
        }

        val cachedAttrs = attrs ?: return
        val service = cachedAttrs.service
        val config = cachedAttrs.config

        val position = position ?: cachedAttrs.initialPosition

        var closestMatrix: Pair<Double, WmtsTileMatrixConfig>? = null
        val targetMetersPerPixel = position.zoom.metersPerPixel
        for (matrix in config.matrices.keys) {
            val possibility = config.set.pixelsPerMeter(matrix)

            if (closestMatrix == null) {
                closestMatrix = possibility to matrix
                continue
            }

            val (closestSoFar, _) = closestMatrix
            if (abs(targetMetersPerPixel - possibility) < abs(targetMetersPerPixel - closestSoFar)) {
                closestMatrix = possibility to matrix
            }
        }
        if (closestMatrix == null) {
            throw IllegalStateException("No possible matrix")
        }
        val (activeMatrixMetersPerPixel, activeMatrix) = closestMatrix
        val metersPerPixelScaleFactor = targetMetersPerPixel / activeMatrixMetersPerPixel
        val scaleFactor = 1 / metersPerPixelScaleFactor

        val (_, centerX, centerY) = position.center.convertTo(config.set.crs)
        val pixelSpan = config.set.pixelsPerMeter(activeMatrix)

        val effectiveWidth = cachedScreenWidth.toDouble() * pixelSpan / scaleFactor
        val effectiveHeight = cachedScreenHeight.toDouble() * pixelSpan / scaleFactor

        val minX = centerX - floor(effectiveWidth / 2.0)
        val maxX = centerX + ceil(effectiveWidth / 2.0)
        val minY = centerY - floor(effectiveHeight / 2.0)
        val maxY = centerY + ceil(effectiveHeight / 2.0)

        val tileRange = config.set.tileIndices(activeMatrix,
            maxCrsX = maxX,
            minCrsX = minX,
            maxCrsY = maxY,
            minCrsY = minY
        )

        val overageX = round(tileRange.minColOverageInCrs / pixelSpan).toInt()
        val overageY = round(tileRange.minRowOverageInCrs / pixelSpan).toInt()

        val requestBuilder = GetTileRequest.Builder.from(service, config, activeMatrix)

        val newTilesToDraw = mutableListOf<TileToDraw>()

        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = (col - tileRange.minColInclusive) * activeMatrix.tileWidth.toInt()
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY =
                    (row - tileRange.minRowInclusive) * activeMatrix.tileHeight.toInt()

                val topLeftX = offsetX - overageX
                val topLeftY = offsetY - overageY
                val width = activeMatrix.tileWidth
                val height = activeMatrix.tileHeight

                val request = requestBuilder.build(row, col)

                newTilesToDraw.add(TileToDraw(
                    topLeftX = topLeftX.toFloat(),
                    topLeftY = topLeftY.toFloat(),
                    width = width.toFloat(),
                    height = height.toFloat(),
                    request = request
                ))
            }
        }

        toDraw = ToDraw(newTilesToDraw.toTypedArray(), scaleFactor.toFloat())

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        val repo = attrs?.repo
        val cachedToDraw = toDraw

        if (canvas == null || cachedToDraw == null || repo == null) {
            // TODO: remove this, added for debugging the hang
            canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                style = Paint.Style.FILL
                color = Color.RED
            })
            return
        }

        val tiles = cachedToDraw.tiles
        val scaleFactor = cachedToDraw.scaleFactor

        Timber.i("Drawing %s tiles at scale factor %s", tiles.size, scaleFactor)

        canvas.scale(scaleFactor, scaleFactor)

        for (tile in tiles) {
            val cached = repo.getCached(tile.request)
            if (cached != null) {
                cached
                    .map {
                        drawTile(canvas, it, tile.topLeftX, tile.topLeftY)
                    }
                    .mapLeft {
                        drawError(canvas, it, tile.topLeftX, tile.topLeftY, tile.width, tile.height)
                    }
            } else {
                drawPlaceholder(canvas, tile.topLeftX, tile.topLeftY, tile.width, tile.height)

                repo.requestCaching(this, tile.request) {
                    // If the tile that just loaded would be visible, redraw
                    val cachedTiles = toDraw?.tiles
                    if (cachedTiles != null) {
                        for (tileToDraw in cachedTiles) {
                            if (tileToDraw.request == tile.request) {
                                invalidate()
                                break
                            }
                        }
                    }
                }
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
