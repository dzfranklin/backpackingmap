package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.layer.MapLayer
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalCoroutinesApi::class)
class MapView(
    private val context: Context,
    private val parent: ViewGroup,
    layerConfigs: Array<WmtsLayerConfig>,
    private val initialPosition: MapPosition,
) : CoroutineScope {
    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    private val layers = layerConfigs.map { config ->
        val view = addView(MapLayer(context))
        view.onAttachToMap(config, initialPosition)
        view
    }

    private val touchHandler = GestureHandler(coroutineContext, context, parent)

    init {
        var last = initialPosition

        launch {
            setLayerPositions(initialPosition)

            touchHandler.events.collect { event ->
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Used for exhaustiveness
                when (event) {
                    is GestureHandler.TouchEvent.Move -> {
                        val zoom = last.zoom
                        val current = MapPosition(
                            zoom = zoom,
                            center = last.center.movedBy(
                                -1 * event.deltaX * zoom.metersPerPixel,
                                event.deltaY * zoom.metersPerPixel
                            )
                        )
                        setLayerPositions(current)
                        last = current
                    }

                    is GestureHandler.TouchEvent.Scale -> {
                        // TODO: Cap max and min scale
                        val current = MapPosition(
                            center = last.center,
                            zoom = last.zoom.scaledBy(event.factor)
                        )
                        setLayerPositions(current)
                        last = current
                    }
                }!!
            }
        }
    }

    private suspend fun setLayerPositions(position: MapPosition) = coroutineScope {
        for (layer in layers) {
            launch {
                layer.onChangePosition(position)
            }
        }
    }

    private fun <T : View> addView(view: T): T {
        parent.addView(view, parent.layoutParams)
        return view
    }
}