package com.backpackingmap.backpackingmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MapLayer : CoroutineScope {
    abstract class Builder<T : MapLayer> {
        abstract fun build(
            mapState: StateFlow<MapState>,
            requestRender: () -> Unit,
            coroutineContext: CoroutineContext,
        ): T
    }

    abstract val render: RenderOperation

    open fun onDetachedFromWindow() {}

    open fun onAttachedToWindow() {}
}