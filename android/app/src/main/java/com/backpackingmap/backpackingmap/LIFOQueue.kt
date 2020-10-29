package com.backpackingmap.backpackingmap

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentLinkedDeque

@OptIn(ExperimentalCoroutinesApi::class)
class LIFOQueue<T> {
    private val queue = ConcurrentLinkedDeque<T>()
    private val newItemNotifier = Channel<Unit>(Channel.UNLIMITED)

    private val flow = flow<T> {
        for (notice in newItemNotifier) {
            val item = queue.removeFirst()
                ?: throw IllegalStateException("Notified of new item but none present")

            emit(item)
        }
    }

    fun enqueue(item: T) {
        queue.addFirst(item)
        if (!newItemNotifier.offer(Unit)) {
            throw IllegalStateException("shouldRecheckQueue should be unlimited, so offer should never fail")
        }
    }

    suspend fun collect(block: suspend (T) -> Unit) {
        flow.collect(block)
    }
}