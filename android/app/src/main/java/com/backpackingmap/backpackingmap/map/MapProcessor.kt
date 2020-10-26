package com.backpackingmap.backpackingmap.map

import com.backpackingmap.backpackingmap.asCrs
import com.backpackingmap.backpackingmap.asNaive
import com.backpackingmap.backpackingmap.asPixel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import org.locationtech.proj4j.CoordinateReferenceSystem
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

@ExperimentalCoroutinesApi
class MapProcessor(
    override val coroutineContext: CoroutineContext,
    initialState: MapState,
) : CoroutineScope {

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    sealed class Event {
        data class Gesture(val event: OmniGestureDetector.Event) : Event()
        data class MoveBy(val deltaX: Float, val deltaY: Float) : Event()
        data class SizeChanged(val size: MapSize) : Event()
        data class ChangeBaseCrs(val newBase: CoordinateReferenceSystem) : Event()
    }

    suspend fun send(event: Event) {
        events.emit(event)
    }

    private fun computeNewState(oldState: MapState, event: Event) = when (event) {
        is Event.Gesture ->
            computeNewStateFromGesture(oldState, event.event)

        is Event.MoveBy ->
            oldState.copy(center = oldState.center
                .asCrs(oldState.baseCrs)
                .movedBy(oldState.zoom, event.deltaX.asPixel(), event.deltaY.asPixel())
                .asNaive()
            )

        is Event.SizeChanged ->
            oldState.copy(size = event.size)

        is Event.ChangeBaseCrs -> {
            if (oldState.baseCrs != event.newBase) {
                val newCenter = oldState.center
                    .asCrs(oldState.baseCrs)
                    .convertTo(event.newBase)
                    .asNaive()
                oldState.copy(baseCrs = event.newBase, center = newCenter)
            } else {
                oldState
            }
        }
    }

    private val events =
        MutableSharedFlow<Event>(EVENT_BUFFER_SIZE, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        launch {
            events.collect { event ->
                _state.value = computeNewState(_state.value, event)
            }
        }
    }

    private var flinger: Job? = null

    private fun computeNewStateFromGesture(
        oldState: MapState,
        event: OmniGestureDetector.Event,
    ): MapState {
        flinger?.cancel("Cancelling fling because of new gesture")

        return when (event) {
            is OmniGestureDetector.Event.Scroll ->
                oldState.copy(center = oldState.center
                    .asCrs(oldState.baseCrs)
                    .movedBy(oldState.zoom, event.distanceX.asPixel(), event.distanceY.asPixel())
                    .asNaive()
                )

            is OmniGestureDetector.Event.Fling -> {
                if (event.velocityX != null && event.velocityY != null) {
                    flinger = launch {
                        var deltaX = -event.velocityX / 15f
                        var deltaY = -event.velocityY / 15f

                        while (abs(deltaX) > 1 || abs(deltaY) > 1) {
                            send(Event.MoveBy(deltaX, deltaY))

                            deltaX *= 0.8f
                            deltaY *= 0.8f

                            delay(1_000 / 60) // 60 fps
                        }
                    }
                }
                oldState
            }

            is OmniGestureDetector.Event.Scale -> {
                if (event.scaleFactor != null) {
                    oldState.copy(zoom = oldState.zoom.scaledBy(1.0 / event.scaleFactor.toDouble()))
                } else {
                    oldState
                }
            }

            else -> oldState
        }
    }

    companion object {
        private const val EVENT_BUFFER_SIZE = 1_000
    }
}