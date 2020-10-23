package com.backpackingmap.backpackingmap.map

interface MapLayer {
    suspend fun computeRender(
        state: MapState,
        requestRerender: () -> Unit,
    ): Collection<RenderOperation>
}