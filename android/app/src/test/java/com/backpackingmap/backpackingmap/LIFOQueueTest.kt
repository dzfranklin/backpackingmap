package com.backpackingmap.backpackingmap

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LIFOQueueTest {
    @Test
    fun `enqueue accepts values`(): Unit = runBlocking {
        val subject = LIFOQueue<Int>()
        subject.enqueue(1)
        subject.enqueue(2)
    }

    @Test
    fun `collect receives values in correct order`() = runBlocking {
        val subject = LIFOQueue<Int>()

        subject.enqueue(1)
        subject.enqueue(2)
        subject.enqueue(3)

        val values = collectToListAsync(subject, 3).await()
        assertThat(values, `is`(listOf(3, 2, 1)))
    }

    @Test
    fun `enqueue handles concurrent calls`() = runBlockingTest {
        launch {
            val subject = LIFOQueue<Int>()
            val callCount = 10_000

            for (n in 0..callCount) {
                launch {
                    subject.enqueue(n)
                }
            }

            var count = 0
            subject.collect {
                if (count == 0) {
                    // The head has a big number
                    assertThat(it, greaterThan(9_000))
                }

                count++

                if (count == callCount) {
                    cancel("Done")
                }
            }

        }
    }

    @Test
    fun `multiple consumers get different items`(): Unit = runBlocking {
        // NOTE: We don't use runBlocking because we need a realistic clock

        launch {
            val subject = LIFOQueue<Int>()

            subject.enqueue(1)
            subject.enqueue(2)

            var totalCallCount = 0

            launch {
                withTimeout(100) {
                    var callCount = 0
                    subject.collect {
                        callCount++
                        totalCallCount++
                        assertThat(callCount, `is`(1))

                        assertThat(it, `is`(2))

                        delay(10) // give the other collector time to get the next item
                    }
                }
            }

            launch {
                withTimeout(100) {
                    var callCount = 0
                    subject.collect {
                        callCount++
                        totalCallCount++
                        assertThat(callCount, `is`(1))

                        assertThat(it, `is`(1))
                    }
                }
            }

            delay(150) // wait for the timeouts
            assertThat(totalCallCount, `is`(2))
        }
    }

   private fun <T> CoroutineScope.collectToListAsync(
       queue: LIFOQueue<T>,
       numValues: Int,
    ): Deferred<List<T>> {
        val values = mutableListOf<T>()

        return async {
            try {
                withContext(Dispatchers.Default) {
                    var collected = 0
                    queue.collect {
                        collected++
                        values.add(it)

                        if (collected >= numValues) {
                            cancel("Done")
                        }
                    }

                    throw IllegalStateException("Unreachable")
                }
            } catch (e: CancellationException) {
                values
            }
        }
    }
}