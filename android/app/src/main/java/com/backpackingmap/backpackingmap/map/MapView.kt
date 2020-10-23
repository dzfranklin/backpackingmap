package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.layer.MapLayer
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MapView(
    private val context: Context,
    private val parent: ViewGroup,
    layerConfigs: Array<WmtsLayerConfig>,
    private val initialPosition: MapPosition,
) : CoroutineScope {
    // TODO: figure out when to cancel to avoid leaks
    override val coroutineContext = CoroutineScope(Dispatchers.Main).coroutineContext

    private val gestureHandler = GestureHandler(coroutineContext, context, parent, initialPosition)

    private val layers = layerConfigs.map { config ->
        val view = addView(MapLayer(context))
        view.onAttachToMap(config, gestureHandler.events)
        view
    }

    private fun <T : View> addView(view: T): T {
        parent.addView(view, parent.layoutParams)
        return view
    }
}