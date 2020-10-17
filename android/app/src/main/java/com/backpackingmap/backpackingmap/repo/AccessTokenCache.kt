package com.backpackingmap.backpackingmap.repo

import arrow.core.Either
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias AccessTokenResponse = Either<RemoteError<RenewSessionResponseError>, AccessToken>

// NOTE: Implementation developed with help from Marc Knaup
// <https://kotlinlang.slack.com/archives/C0922A726/p1602866129274400>
class AccessTokenCache(private val renew: suspend () -> AccessTokenResponse) {
    private var cache: AccessToken? = null

    private val getMutex = Mutex()

    suspend fun get(): AccessTokenResponse {
        val tempInstance = cache
        if (tempInstance != null) {
            return Either.right(tempInstance)
        }

        getMutex.withLock {
            // NOTE: We check a second time because since we just waited for a lock the previous
            // owner of the lock might have renewed
            val instance = cache
            return if (instance != null) {
                Either.right(instance)
            } else {
                renew().map {
                    cache = it
                    it
                }
            }
        }
    }

    fun prime() {
        runBlocking {
            launch {
                get()
            }
        }
    }
}