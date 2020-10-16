package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.repo.Repo

class MapView(
    private val context: Context,
    private val parent: ViewGroup,
    private val service: WmtsServiceConfig,
    layerConfigs: Array<WmtsLayerConfig>,
    private val extents: MapExtents,
    private val repo: Repo,
) {
    private val layers = layerConfigs.map { config ->
        MapLayer(context, parent, service, config, extents, repo)
    }
}