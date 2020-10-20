package com.backpackingmap.backpackingmap.repo

import arrow.core.Either
import com.backpackingmap.backpackingmap.net.AccessToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AccessTokenCacheTest {
    private val successResponse = Either.right(AccessToken("TOKEN"))
    private val errorResponse = Either.left(UnauthenticatedRemoteError.Network(Throwable("Error")))

    @Test
    fun `get with success response returns success`() {
        val subject = AccessTokenCache { successResponse }
        runBlocking {
            assertThat(subject.get(), `is`(successResponse))
        }
    }

    @Test
    fun `get with error response return error`() {
        val subject = AccessTokenCache { errorResponse }
        runBlocking {
            assertThat(subject.get(), `is`(errorResponse))
        }
    }

    @Test
    fun `refresh with success response return Unit`() {
        val subject = AccessTokenCache { successResponse }
        runBlocking {
            assertThat(subject.refresh(), `is`(Either.right(Unit)))
        }
    }

    @Test
    fun `can get cached after failed refresh`() {
        var response: AccessTokenResponse = successResponse
        val subject = AccessTokenCache { response }
        runBlocking {
            assertThat(subject.get(), `is`(successResponse))
            response = errorResponse
            assertThat(subject.refresh(), `is`(errorResponse))
            assertThat(subject.get(), `is`(successResponse))
        }
    }

    @Test
    fun `handles many concurrent calls without refreshing multiple times`() {
        val refreshCount = AtomicInteger(0)

        val subject = AccessTokenCache {
            refreshCount.incrementAndGet()
            successResponse
        }

        runBlocking {
            (0..10_000)
                .map {
                    async {
                        assertThat(subject.get(), `is`(successResponse))
                    }
                }
                .awaitAll()
        }

        assertThat(refreshCount.get(), `is`(1))
    }

    @Test
    fun `Only refreshes once if called repeatedly around the same time`() {
        val refreshCount = AtomicInteger(0)

        val subject = AccessTokenCache {
            refreshCount.incrementAndGet()
            successResponse
        }

        runBlocking {
            (0..10_000)
                .map {
                    async {
                        assertThat(subject.refresh(), `is`(Either.right(Unit)))
                    }
                }
                .awaitAll()
        }

        assertThat(refreshCount.get(), `is`(1))
    }
}