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
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
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

    private val requested = HashSet<GetTileRequest>()

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

        val requestBuilder = GetTileRequest.Builder.from(service, config, activeMatrix)

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

                val cached = repo.getCached(request)
                if (cached != null) {
                    cached
                        .map {
                            drawTile(canvas, it, topLeftX, topLeftY)
                        }
                        .mapLeft {
                            drawError(canvas, it, topLeftX, topLeftY, width, height)
                        }
                } else {
                    drawPlaceholder(canvas, topLeftX, topLeftY, width, height)

                    if (!requested.contains(request)) {
                        repo.requestCaching(request) {
                            invalidate()
                        }
                        requested.add(request)
                    }
                }
            }
        }
    }

    private val density = resources.displayMetrics.density

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

    private val errorPaint = TextPaint().apply {
        color = Color.RED
        textSize = 20F * density
    }

    private val errorPadding = 5F * density

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

    private fun drawError(
        canvas: Canvas,
        error: GetTileError,
        topLeftX: Int,
        topLeftY: Int,
        width: Pixel,
        height: Pixel,
    ) {
        val text = error.toString()

        val axisPadding = (2 * errorPadding).toInt()

        val internalWidth = width.toInt() - axisPadding.toInt()

        val rect = Rect()
        errorPaint.getTextBounds(text, 0, text.length, rect)
        val textLineHeight = rect.height()
        val internalHeightInLines =
            floor((height.toFloat() - axisPadding) / textLineHeight).toInt()

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, errorPaint, internalWidth)
            .setMaxLines(internalHeightInLines)
            .build()
        canvas.withTranslation(topLeftX + errorPadding, topLeftY + errorPadding) {
            layout.draw(this)
        }
    }
}

// TODO: use
class TileNoLongerNeeded() : CancellationException("Tile no longer needed")
