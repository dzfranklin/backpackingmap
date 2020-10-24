package com.backpackingmap.backpackingmap.map

import arrow.syntax.collections.tail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MapRenderer constructor(
    override val coroutineContext: CoroutineContext,
    private val layers: MutableStateFlow<List<MapLayer>>,
) : CoroutineScope {
    val operation: StateFlow<RenderOperation> = layers
        // for each set of layers
        .transform<List<MapLayer>, RenderOperation> { layers ->
            // turn the layers into a list of render operations
            val renderers = layers.map { it.render }

            val head: Flow<RenderOperation>? = renderers.firstOrNull()
            if (head != null) {
                // if there are >0 layers, combine the render operations
                val tail = renderers.tail()
                tail.fold(head) { acc: Flow<RenderOperation>, item: Flow<RenderOperation> ->
                    acc
                        .combine(item) { a, b -> a + b }
                }
            } else {
                // otherwise use the no-op operation
                listOf(UnitRenderOperation).asFlow()
            }
        }
        // convert the Flow to a StateFlow
        .stateIn(this, SharingStarted.Eagerly, UnitRenderOperation)
}