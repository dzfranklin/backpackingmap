package com.backpackingmap.backpackingmap.repo

import arrow.core.Either
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*

typealias AccessTokenResponse = Either<UnauthenticatedRemoteError<RenewSessionResponseError>, AccessToken>

// NOTE: Implementation developed with help from Marc Knaup
// <https://kotlinlang.slack.com/archives/C0922A726/p1602866129274400>
class AccessTokenCache(private val renew: suspend () -> AccessTokenResponse) {
    private var cache: AccessToken? = null
    private var cacheLastUpdated: Instant? = null

    private val getMutex = Mutex()
    private val refreshMutex = Mutex()

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
                renewAndCache()
            }
        }
    }

    suspend fun refresh(): Either<UnauthenticatedRemoteError<RenewSessionResponseError>, Unit> {
        refreshMutex.withLock {
            val now = Clock.System.now()
            val tempInstance = cacheLastUpdated
            if (tempInstance == null ||
                tempInstance.until(now, DateTimeUnit.MINUTE, TimeZone.UTC)
                > MAX_MINUTES_BETWEEN_REFRESHES
            ) {
                return renewAndCache().map {
                    cacheLastUpdated = Clock.System.now()
                    Unit
                }
            } else {
                return Either.right(Unit)
            }
        }
    }

    private suspend fun renewAndCache(): AccessTokenResponse =
        renew().map {
            cache = it
            it
        }

    companion object {
        private const val MAX_MINUTES_BETWEEN_REFRESHES = 1
//        private const val MAX_MINUTES_BETWEEN_REFRESHES = 15
    }
}