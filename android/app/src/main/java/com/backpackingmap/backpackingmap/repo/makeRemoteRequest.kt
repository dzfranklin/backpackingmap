package com.backpackingmap.backpackingmap.repo

import arrow.core.Either
import com.backpackingmap.backpackingmap.net.Response
import retrofit2.HttpException
import timber.log.Timber

suspend fun <ApiData, ApiError> makeRemoteRequest(
    requester: suspend () -> Response<ApiData, ApiError>,
): Either<RemoteError<ApiError>, ApiData> {
    val out = try {
        val response = requester()
        when {
            response.error != null ->
                Either.left(RemoteError.Api(response.error))
            response.data != null ->
                Either.right(response.data)
            else ->
                Either.left(RemoteError.Server("Invalid response format",
                    IllegalStateException("Response has neither error nor data")))
        }
    } catch (throwable: HttpException) {
        Either.left(RemoteError.Server("Status ${throwable.code()}", throwable))
    } catch (throwable: Throwable) {
        Either.left(RemoteError.Network(throwable))
    }

    if (out is Either.Left) {
        Timber.w("Got RemoteError: %s:", out.a)
    }

    return out
}