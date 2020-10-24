package com.backpackingmap.backpackingmap

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class LIFOQueue<T> {
    private val queue = ArrayDeque<T>()
    private val shouldRecheckQueue = Channel<Unit>(Channel.UNLIMITED)

    private val flow = flow<T> {
        for (msg in shouldRecheckQueue) {
            currentCoroutineContext().ensureActive()

            val item = queue.removeFirstOrNull()
            if (item != null) {
                emit(item)
            }
        }
    }

    fun enqueue(item: T) {
        queue.addFirst(item)
        if (!shouldRecheckQueue.offer(Unit)) {
            throw IllegalStateException("shouldRecheckQueue should be unlimited, so offer should never fail")
        }
    }

    suspend fun collect(block: suspend (T) -> Unit) {
        flow.collect(block)
    }
}