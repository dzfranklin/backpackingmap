package com.backpackingmap.backpackingmap.net

import arrow.core.Either
import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError
import kotlinx.coroutines.runBlocking
import okhttp3.*
import timber.log.Timber

class ApiTokenInterceptor(private val token: RenewalToken) : Interceptor {
    private val unauthenticatedApiService = UnauthenticatedApi.createService()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        Timber.i("Attempting to add auth tokens to request to %s", originalRequest.url())

        return when (val accessToken = getAccessToken()) {
            is Either.Left -> buildAccessTokenErrorResponse(chain, accessToken.a)
            is Either.Right -> getResponseUsingAccessToken(chain, accessToken.b)
        }

    }

    private fun buildAccessTokenErrorResponse(
        chain: Interceptor.Chain,
        error: RenewSessionResponseError,
    ) =
        Response.Builder()
            .code(503)
            .message("Service Unavailable")
            .body(ResponseBody.create(MediaType.get("text/plain"),
                "Error in ApiTokenInterceptor: Got ${error.message} when trying to renew access token"))
            .protocol(Protocol.HTTP_1_0)
            .request(chain.request())
            .build()

    private fun getResponseUsingAccessToken(
        chain: Interceptor.Chain,
        accessToken: String,
    ): Response {
        val originalRequest = chain.request()

        val request = originalRequest.newBuilder()
            .header("Authorization", accessToken)
            .build()

        Timber.i("Added auth token to request %s, proceeding", originalRequest.url())

        return chain.proceed(request)
    }

    private var accessTokenCache: String? = null

    private fun getAccessToken(): Either<RenewSessionResponseError, String> {
        val cached = accessTokenCache
        return if (cached != null) {
            Either.right(cached)
        } else {
            runBlocking { getNewAccessToken() }
        }
    }

    private suspend fun getNewAccessToken(): Either<RenewSessionResponseError, String> {
        val response = unauthenticatedApiService.renewSession(token)
        return when {
            response.error != null -> {
                Timber.w("Got error when trying to renew access token: %s", response.error)
                Either.left(response.error)
            }
            response.data != null -> {
                Timber.i("Got new access token")
                accessTokenCache = response.data.access_token
                Either.right(response.data.access_token)
            }
            else -> {
                throw IllegalStateException("Neither data nor error")
            }
        }
    }
}
