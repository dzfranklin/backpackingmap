package com.backpackingmap.backpackingmap.map

import android.graphics.Canvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MapRenderer constructor(
    override val coroutineContext: CoroutineContext,
    private val layers: MutableStateFlow<List<MapLayer>>,
) : CoroutineScope {
    fun renderTo(canvas: Canvas) {
        for (layer in layers.value) {
            layer.render.renderTo(canvas)
        }
    }
}