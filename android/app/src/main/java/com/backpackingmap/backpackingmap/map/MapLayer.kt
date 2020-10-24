package com.backpackingmap.backpackingmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MapLayer : CoroutineScope {
    abstract class Builder {
        abstract fun build(mapState: StateFlow<MapState>, coroutineContext: CoroutineContext): MapLayer
    }

    abstract val render: StateFlow<RenderOperation>
}