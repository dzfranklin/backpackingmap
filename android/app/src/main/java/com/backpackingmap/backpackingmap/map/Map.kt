package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.SurfaceView
import android.view.ViewGroup

class Map(
    private val context: Context,
    private val parent: ViewGroup,
    private val layerConfigs: List<MapLayerConfig>,
    private val extents: MapExtents,
) {
    private val layers = layerConfigs.map { config ->
        val surface = SurfaceView(context)
        val holder = surface.holder

        parent.addView(surface, parent.layoutParams)
        holder.setFixedSize(extents.surfaceWidth(), extents.surfaceHeight())

        MapLayer(holder, config, extents)
    }
}