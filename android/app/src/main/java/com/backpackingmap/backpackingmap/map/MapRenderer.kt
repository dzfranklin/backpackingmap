package com.backpackingmap.backpackingmap.map

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
class MapRenderer constructor(
    override val coroutineContext: CoroutineContext,
    private val state: StateFlow<MapState>,
    private val layers: Collection<MapLayer>,
) : CoroutineScope {
    private val _operations: MutableStateFlow<Map<MapLayer, Collection<RenderOperation>>> =
        MutableStateFlow(mapOf())
    val operations get() = _operations.asStateFlow()

    init {
        launch {
            state.collect {
                actor.send(Event.NewState(it))
            }
        }
    }

    sealed class Event {
        data class NewState(val state: MapState) : Event()
        data class RerenderLayer(val layer: MapLayer) : Event()
    }

    private val actor = actor<Event> {
        var lastState = state.value

        for (event in channel) {
            _operations.value = when (event) {
                is Event.NewState -> {
                    lastState = event.state

                    layers
                        .map { layer ->
                            async {
                                layer.computeRender(event.state, createLayerRerenderer(layer))
                            }
                        }
                        .awaitAll()
                        .zip(layers)
                        .map { (operations, layer) -> layer to operations }
                        .toMap()
                }

                is Event.RerenderLayer -> {
                    val layer = event.layer
                    val mutable = _operations.value.toMutableMap()
                    mutable[layer] = layer.computeRender(lastState, createLayerRerenderer(layer))
                    mutable.toMap()
                }
            }
        }
    }

    private fun createLayerRerenderer(layer: MapLayer): () -> Unit {
        return { ->
            launch {
                actor.send(Event.RerenderLayer(layer))
            }
        }
    }
}