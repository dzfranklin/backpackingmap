package com.backpackingmap.backpackingmap.repo

import com.backpackingmap.backpackingmap.net.auth.RenewSessionResponseError

sealed class RemoteError<out ApiErrorResponse> {

    data class Network(val cause: Throwable) :
        RemoteError<Nothing>()

    data class Server(val type: String, val cause: Throwable) :
        RemoteError<Nothing>()

    data class AcquiringAuth(val cause: UnauthenticatedRemoteError<RenewSessionResponseError>) :
        RemoteError<Nothing>()

    data class Auth(val cause: Throwable) : RemoteError<Nothing>()

    data class Api<ApiErrorResponse>(val response: ApiErrorResponse) :
        RemoteError<ApiErrorResponse>()
}