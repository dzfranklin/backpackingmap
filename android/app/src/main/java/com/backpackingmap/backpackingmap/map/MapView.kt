package com.backpackingmap.backpackingmap.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.backpackingmap.backpackingmap.Coordinate
import com.backpackingmap.backpackingmap.asNaive
import com.backpackingmap.backpackingmap.asPixel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Supporting construction from just context & params would complicate the state without
// much benefit
@SuppressLint("ViewConstructor")
@OptIn(ExperimentalCoroutinesApi::class)
class MapView(
    context: Context,
    initialCenter: Coordinate,
    initialZoom: ZoomLevel,
    locationProcessor: ForegroundLocationProcessor,
) : View(context), CoroutineScope {
    // TODO: figure out when to cancel to avoid leaks
    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    private val gestureDetector = OmniGestureDetector(context)

    // Not applicable, as we just delegate to platform GestureDetectors
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(this, event)
    }

    private val initialState = MapState(
        baseCrs = initialCenter.crs,
        center = initialCenter.asNaive(),
        zoom = initialZoom,
        size = MapSize(width.asPixel(), height.asPixel())
    )
    private val processor = MapProcessor(coroutineContext, initialState)

    init {
        // Process gestures
        gestureDetector.events
            .onEach {
                processor.send(MapProcessor.Event.Gesture(it))
            }
            .launchIn(this)
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        launch {
            processor.send(MapProcessor.Event.SizeChanged(MapSize(width.asPixel(),
                height.asPixel())))
        }
    }

    val baseLayer: MutableStateFlow<BaseMapLayer.Builder<BaseMapLayer>?> = MutableStateFlow(null)
    private val _baseLayer = baseLayer
        .map { builder ->
            if (builder != null) {
                processor.send(MapProcessor.Event.ChangeBaseCrs(builder.baseCrs))
                createLayer(builder)
            } else {
                null
            }
        }
        .stateIn(this, SharingStarted.Eagerly, null)

    /** Custom layers placed over the base layer */
    val layers = MutableStateFlow<List<MapLayer.Builder<MapLayer>>>(emptyList())

    private val customLayers = layers
        .scan(emptyList<MapLayer>()) { prev, builders ->
            prev.forEach { it.coroutineContext.cancel(CancellationException("Layers changed")) }
            builders.map(::createLayer)
        }
        .stateIn(this, SharingStarted.Eagerly, emptyList())

    private val myPositionLayer = createLayer(MyPositionLayer.Builder(context, locationProcessor))
    private val topBuiltinLayers = MutableStateFlow<List<MapLayer>>(listOf(myPositionLayer))

    private val allLayers = _baseLayer
        .combine(customLayers) { base, custom ->
            if (base == null) {
                custom
            } else {
                listOf(base) + custom
            }
        }
        .combine(topBuiltinLayers) { rest, top ->
            rest + top
        }
        .stateIn(this, SharingStarted.Eagerly, emptyList())

    private fun <T : MapLayer> createLayer(builder: MapLayer.Builder<T>): T =
        // Create job so we can cancel layers separately
        builder.build(processor.state, ::postInvalidate, coroutineContext + Job())

    private val renderer = MapRenderer(coroutineContext, allLayers)

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }

        renderer.renderTo(canvas)
    }

    override fun onDetachedFromWindow() {
        for (layer in allLayers.value) {
            layer.onDetachedFromWindow()
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        for (layer in allLayers.value) {
            layer.onAttachedToWindow()
        }
        super.onAttachedToWindow()
    }
}
