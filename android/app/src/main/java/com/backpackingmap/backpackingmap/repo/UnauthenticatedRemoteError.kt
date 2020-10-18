package com.backpackingmap.backpackingmap.repo

sealed class UnauthenticatedRemoteError<out ApiErrorResponse>() {

    data class Network(val cause: Throwable) :
        UnauthenticatedRemoteError<Nothing>()

    data class Server(val type: String, val cause: Throwable) :
        UnauthenticatedRemoteError<Nothing>()

    data class Api<ApiErrorResponse>(val response: ApiErrorResponse) :
        UnauthenticatedRemoteError<ApiErrorResponse>()
}