package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTilePosition
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class MapLayer(
    private val context: Context,
    parent: ViewGroup,
    private val service: WmtsServiceConfig,
    private val config: WmtsLayerConfig,
    private val size: MapSize,
    private val position: Flow<MapPosition>,
    private val repo: Repo,
) {
    private val surface = SurfaceView(context)
    private val holder: SurfaceHolder = surface.holder
    private lateinit var scope: CoroutineScope

    init {
        parent.addView(surface, parent.layoutParams)
        holder.setFixedSize(size.screenWidth.toInt(), size.screenHeight.toInt())

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Timber.i("Surface created")

                scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    drawOnChange()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Timber.i("Surface destroyed")

                scope.cancel()
            }
        })
    }

    private val tiles: MutableMap<WmtsTilePosition, Bitmap> = mutableMapOf()
    private val pendingTiles: MutableMap<WmtsTilePosition, Int> = mutableMapOf()
    private var lastDraw = AtomicInteger(0)

    private suspend fun drawOnChange() {
        position.collect { mapPosition ->
            val currentDraw = lastDraw.incrementAndGet()

            val activeMatrix = config.matrices.keys.last() // TODO: Set based on zoom

            val (_, centerX, centerY) = mapPosition.center.convertTo(config.set.crs)
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

            val canvas =
                withContext(Dispatchers.Main) { holder.lockCanvas() }

            // TODO: delete old tiles

            for (col in tileRange.minColInclusive..tileRange.maxColInclusive) {
                val offsetX = (col - tileRange.minColInclusive) * activeMatrix.tileWidth.toInt()
                for (row in tileRange.minRowInclusive..tileRange.maxRowInclusive) {
                    val offsetY =
                        (row - tileRange.minRowInclusive) * activeMatrix.tileHeight.toInt()
                    val position = WmtsTilePosition(row = row, col = col)

                    val placement = TilePlacement(
                        topLeftX = offsetX - overageX,
                        topLeftY = offsetY - overageY,
                        width = activeMatrix.tileWidth,
                        height = activeMatrix.tileHeight
                    )

                    val cached = tiles[position]
                    if (cached != null) {
                        drawTile(canvas, placement, cached)
                    } else {
                        drawPlaceholder(canvas, placement)
                        if (pendingTiles[position] != null) {
                            pendingTiles[position] = currentDraw
                        } else {
                            pendingTiles[position] = currentDraw

                            scope.launch {
                                repo.getTile(service, config, config.set, activeMatrix, position)
                                    .map {
                                        val scheduledForDraw = pendingTiles[position]
                                        if (scheduledForDraw != null && scheduledForDraw == lastDraw.get()) {
                                            withContext(Dispatchers.Main) {
                                                val canvas = holder.lockCanvas(placement.toRect())
                                                if (canvas != null) {
                                                    drawTile(canvas, placement, it)
                                                } else {
                                                    Timber.w("Failed to lock canvas to draw received tile")
                                                }
                                            }
                                        }
                                        tiles[position] = it
                                    }
                                    .mapLeft {
                                        Timber.w("Error fetching tile: %s", position)
                                    }
                            }
                        }
                    }


                }
            }

            withContext(Dispatchers.Main) { holder.unlockCanvasAndPost(canvas) }
        }
    }

    private suspend fun drawPlaceholder(canvas: Canvas, placement: TilePlacement) =
        withContext(Dispatchers.Main) {
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = context.getColor(R.color.unloadedTile)

            canvas.drawRect(placement.toRect(), paint)
        }

    private suspend fun drawTile(
        canvas: Canvas,
        placement: TilePlacement,
        tile: Bitmap,
    ) = withContext(Dispatchers.Main) {
        canvas.drawBitmap(tile,
            placement.topLeftX.toFloat(),
            placement.topLeftY.toFloat(),
            null
        )
    }
}
