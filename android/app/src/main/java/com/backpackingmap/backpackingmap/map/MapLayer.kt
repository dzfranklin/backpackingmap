package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.graphics.withTranslation
import com.backpackingmap.backpackingmap.R
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
    }

    fun onChangePosition(newPosition: MapPosition) {
        position = newPosition
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null || !attached || !sized) {
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
        val scaleFactor = 1 / metersPerPixelScaleFactor

        val (_, centerX, centerY) = cachedPosition.center.convertTo(cachedConfig.set.crs)
        val pixelSpan = cachedConfig.set.metersPerPixel(matrix).toFloat()

        val effectiveWidth = cachedScreenWidth * pixelSpan / scaleFactor
        val effectiveHeight = cachedScreenHeight * pixelSpan / scaleFactor

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

        canvas.scale(scaleFactor, scaleFactor)

        val width = matrix.tileWidth.toFloat()
        val height = matrix.tileHeight.toFloat()

        for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
            val offsetX = (col - tileRange.minColInclusive).toFloat() * matrix.tileWidth.toFloat()
            for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                val offsetY =
                    (row - tileRange.minRowInclusive).toFloat() * matrix.tileHeight.toFloat()

                val topLeftX = offsetX - overageX
                val topLeftY = offsetY - overageY

                val request = requestBuilder.build(row, col)

                val cached = repo.getCached(request)
                if (cached != null) {
                    cached
                        .map {
                            drawTile(canvas, it, topLeftX, topLeftY)
                        }
                        .mapLeft {
                            drawError(canvas,
                                it,
                                topLeftX,
                                topLeftY,
                                width,
                                height)
                        }
                } else {
                    drawPlaceholder(canvas, topLeftX, topLeftY, width, height)

                    repo.requestCaching(this, request) {}
                }
            }
        }

        postInvalidateOnAnimation()
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
        private const val DRAW_DEBUG_TILE_BOXES = true
    }
}
