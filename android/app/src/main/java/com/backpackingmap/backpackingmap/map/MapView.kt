package com.backpackingmap.backpackingmap.map

import android.content.Context
import android.view.ViewGroup
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class MapView(
    private val context: Context,
    private val parent: ViewGroup,
    private val service: WmtsServiceConfig,
    layerConfigs: Array<WmtsLayerConfig>,
    private val size: MapSize,
    private val initialPosition: MapPosition,
    private val repo: Repo,
) {
    private val position = flow {
        var last = initialPosition
        while (true) {
            delay(1000 / 60)
            val position = MapPosition(
                zoom = last.zoom,
                center = Coordinate(
                    crs = last.center.crs,
                    x = last.center.x - 0.0001,
                    y = last.center.y
                )
            )
            emit(position)
            last = position
        }
    }

    private val layers = layerConfigs.map { config ->
        MapLayer(
            context = context,
            parent = parent,
            service = service,
            config = config,
            size = size,
            position = position,
            repo = repo
        )
    }
}