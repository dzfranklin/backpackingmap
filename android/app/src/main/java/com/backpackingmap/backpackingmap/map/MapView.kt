package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MapView(
    private val context: Context,
    private val parent: ViewGroup,
    layerConfigs: Array<WmtsLayerConfig>,
    private val initialPosition: MapPosition,
    private val repo: Repo,
) : CoroutineScope {
    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    private val layers = layerConfigs.map { config ->
        val view = addView(MapLayer(context))
        view.onReceiveAttrs(MapLayer.Attrs(
            config = config,
            initialPosition = initialPosition,
            repo = repo.tileRepo,
        ))
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

    private fun setLayerPositions(position: MapPosition) {
        for (layer in layers) {
            layer.onChangePosition(position)
        }
    }

    private fun <T : View> addView(view: T): T {
        parent.addView(view, parent.layoutParams)
        return view
    }
}