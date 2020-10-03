package com.backpackingmap.backpackingmap.repository

sealed class RemoteError<out ApiErrorResponse>() {

    data class Network(val cause: Throwable) :
        RemoteError<Nothing>()

    data class Server(val type: String, val cause: Throwable) :
        RemoteError<Nothing>()

    data class Api<ApiErrorResponse>(val response: ApiErrorResponse) :
        RemoteError<ApiErrorResponse>()
}