package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.backpackingmap.backpackingmap.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class MyPositionLayer(
    private val context: Context,
    private val mapState: StateFlow<MapState>,
    private val requestRender: () -> Unit,
    private val locationProcessor: ForegroundLocationProcessor,
    override val coroutineContext: CoroutineContext,
) : MapLayer() {
    data class Builder(
        private val context: Context,
        private val locationProcessor: ForegroundLocationProcessor,
    ) : MapLayer.Builder<MyPositionLayer>() {
        override fun build(
            mapState: StateFlow<MapState>,
            requestRender: () -> Unit,
            coroutineContext: CoroutineContext,
        ) = MyPositionLayer(context, mapState, requestRender, locationProcessor, coroutineContext)
    }

    override var render: RenderOperation = UnitRenderOperation

    private var job: Job? = null
    private var shouldBeActive = false

    private fun activate() {
        shouldBeActive = true

        job?.cancel(CancellationException("Only on job at a time"))

        job = launch {
            locationProcessor.location
                .combine(mapState) { location, mapState ->
                    mapState to location
                }
                .collectLatest { (mapState, location) ->
                    render = computeRender(mapState, location)
                    requestRender()
                }
        }
    }

    init {
        activate()
    }

    private fun deactivate() {
        shouldBeActive = false
        job?.cancel(CancellationException("Deactivating"))
    }

    override fun onDetachedFromWindow() {
        if (shouldBeActive) {
            deactivate()
        }
    }

    override fun onAttachedToWindow() {
        if (shouldBeActive) {
            activate()
        }
    }

    private fun computeRender(mapState: MapState, location: GPSLocation) =
        when (location) {
            is GPSLocation.Known -> {
                // TODO: Don't render if completely off screen?
                val position = location.coordinate.toScreenLocation(mapState)
                createRender(position, location.coordinateAccuracy, mapState)
            }

            else -> UnitRenderOperation
        }

    private fun createRender(
        screenLocation: ScreenLocation,
        accuracy: Float?,
        mapState: MapState,
    ) =
        RenderMyLocation(
            x = screenLocation.x.toFloat(),
            y = screenLocation.y.toFloat(),
            meRadius = ME_RADIUS,
            mePaint = mePaint,
            meBorderWidth = ME_BORDER_WIDTH,
            meBorderPaint = meBorderPaint,
            uncertaintyRadius = computeUncertaintyRadius(accuracy, mapState),
            uncertaintyPaint = uncertaintyPaint,
        )

    private fun computeUncertaintyRadius(nullableAccuracy: Float?, mapState: MapState): Pixel {
        val accuracy =
            nullableAccuracy?.toDouble()?.asMeters() ?: Pixel(maxOf(mapState.size.width.value,
                mapState.size.height.value)) * mapState.zoom.level
        return accuracy * mapState.zoom.level.inverse()
    }

    private val mePaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.myLocation)
    }

    private val meBorderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.myLocationBorder)
    }

    private val uncertaintyPaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.myLocationUncertainty)
    }

    private data class RenderMyLocation(
        val x: Float,
        val y: Float,
        val meRadius: Pixel,
        val mePaint: Paint,
        val meBorderWidth: Pixel,
        val meBorderPaint: Paint,
        val uncertaintyRadius: Pixel,
        val uncertaintyPaint: Paint,
    ) : RenderOperation {
        override fun renderTo(canvas: Canvas) {
            canvas.drawCircle(x, y, uncertaintyRadius.toFloat(), uncertaintyPaint)
            canvas.drawCircle(x, y, (meRadius + meBorderWidth).toFloat(), meBorderPaint)
            canvas.drawCircle(x, y, meRadius.toFloat(), mePaint)
        }
    }

    companion object {
        private val ME_RADIUS = Pixel(20)
        private val ME_BORDER_WIDTH = Pixel(2)
    }
}