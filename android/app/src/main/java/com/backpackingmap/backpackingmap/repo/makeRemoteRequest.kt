package com.backpackingmap.backpackingmap.repo

import arrow.core.Either
import arrow.core.flatMap
import com.backpackingmap.backpackingmap.net.AccessToken
import com.backpackingmap.backpackingmap.net.Response
import okhttp3.ResponseBody
import retrofit2.HttpException
import timber.log.Timber

private const val CODE_NOT_AUTHENTICATED = 401

suspend fun <ApiData, ApiError> makeUnauthenticatedRemoteRequest(
    requester: suspend () -> Response<ApiData, ApiError>,
): Either<UnauthenticatedRemoteError<ApiError>, ApiData> =
    mapThrownErrorsUnauthenticated {
        val response = requester()
        when {
            response.error != null ->
                Either.left(UnauthenticatedRemoteError.Api(response.error))
            response.data != null ->
                Either.right(response.data)
            else ->
                Either.left(UnauthenticatedRemoteError.Server("Invalid response format",
                    IllegalStateException("Response has neither error nor data")))
        }
    }

suspend fun makeRemoteRequestForBody(
    access: AccessTokenCache,
    requester: suspend (token: AccessToken) -> ResponseBody,
): Either<RemoteError<Nothing>, ResponseBody> {
    val token = when (val tokenResponse = access.get()) {
        is Either.Left ->
            return Either.left(RemoteError.AcquiringAuth(tokenResponse.a))
        is Either.Right ->
            tokenResponse.b
    }

    val attempt = mapThrownErrors<Nothing, ResponseBody> {
        Either.right(requester(token))
    }

    return if (attempt is Either.Left && attempt.a is RemoteError.Auth) {
        Timber.i("Got RemoteError.Auth, refreshing and retrying once")
        access.refresh()
            .mapLeft {
                RemoteError.AcquiringAuth(it)
            }
            .flatMap {
                mapThrownErrors {
                    Either.right(requester(token))
                }
            }
    } else {
        attempt
    }
}

private suspend fun <ApiError, Success> mapThrownErrorsUnauthenticated(
    perform: suspend () -> Either<UnauthenticatedRemoteError<ApiError>, Success>,
): Either<UnauthenticatedRemoteError<ApiError>, Success> {
    val out = try {
        perform()
    } catch (throwable: HttpException) {
        Either.left(UnauthenticatedRemoteError.Server("Status ${throwable.code()}", throwable))
    } catch (throwable: Throwable) {
        Either.left(UnauthenticatedRemoteError.Network(throwable))
    }

    if (out is Either.Left) {
        Timber.w("Got RemoteError: %s:", out.a)
    }

    return out
}

private suspend fun <ApiError, Success> mapThrownErrors(
    perform: suspend () -> Either<RemoteError<ApiError>, Success>,
): Either<RemoteError<ApiError>, Success> {
    val out = try {
        perform()
    } catch (throwable: HttpException) {
        if (throwable.code() == CODE_NOT_AUTHENTICATED) {
            Either.left(RemoteError.Auth(throwable))
        } else {
            Either.left(RemoteError.Server("Status ${throwable.code()}", throwable))
        }
    } catch (throwable: Throwable) {
        Either.left(RemoteError.Network(throwable))
    }

    if (out is Either.Left) {
        Timber.w("Got RemoteError: %s:", out.a)
    }

    return out
}