package com.backpackingmap.backpackingmap.map.layer

import android.content.Context
import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.graphics.withTranslation
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.MapPosition
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.repo.GetTileError
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.floor
import kotlin.properties.Delegates

@OptIn(ExperimentalCoroutinesApi::class)
class MapLayer constructor(context: Context) : View(context) {
    val repo = Repo.fromContext(context)!!.tileRepo

    val model = MapLayerModel(repo, ::postInvalidate)

    private var screenHeight by Delegates.notNull<Int>()
    private var screenWidth by Delegates.notNull<Int>()
    private var sized = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        model.onSizeChanged(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun onAttachToMap(config: WmtsLayerConfig, position: SharedFlow<MapPosition>) {
        model.onAttachToMap(config, position)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        canvas.scale(model.scaleFactor, model.scaleFactor)

        for (tile in model.tiles) {
            when (tile.value) {
                is MapLayerModel.TileType.Raster ->
                    drawTile(canvas, tile.value.bitmap, tile.leftX, tile.topY)

                is MapLayerModel.TileType.Error ->
                    drawError(canvas,
                        tile.value.error,
                        tile.leftX,
                        tile.topY,
                        tile.width,
                        tile.height)

                is MapLayerModel.TileType.Placeholder ->
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
