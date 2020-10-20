package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
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
    private val service: WmtsServiceConfig,
    layerConfigs: Array<WmtsLayerConfig>,
    private val size: MapSize,
    private val initialPosition: MapPosition,
    private val repo: Repo,
) : CoroutineScope {
    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    private val layers = layerConfigs.map { config ->
        val view = addView(MapLayer(context))
        view.onReceiveAttrs(MapLayer.Attrs(
            service = service,
            config = config,
            size = size,
            initialPosition = initialPosition,
            repo = repo.tileRepo,
        ))
        view
    }

    private val touchHandler = TouchHandler(coroutineContext, parent)

    init {
        var last = initialPosition

        launch {
            setLayerPositions(initialPosition)

            touchHandler.events.collect { event ->
                when (event) {
                    is TouchHandler.TouchEvent.Move -> {
                        val current = MapPosition(
                            zoom = last.zoom,
                            center = Coordinate(
                                crs = last.center.crs,
                                x = last.center.x - (event.delta.x / 10000),
                                y = last.center.y + (event.delta.y / 10000)
                            )
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