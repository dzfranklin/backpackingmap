package com.backpackingmap.backpackingmap.repo

import com.backpackingmap.backpackingmap.net.ResponseErrorWithMessage

sealed class GetTileError {
    data class Network(val cause: Throwable) : GetTileError()

    data class Server(val type: String, val cause: Throwable) : GetTileError()

    data class ApiAuth(val response: ResponseErrorWithMessage) : GetTileError()

    // TODO: Add errrors for caching when that's added
}

fun <T : ResponseErrorWithMessage> getTileErrorFromRemoteError(error: RemoteError<T>): GetTileError =
    when (error) {
        is RemoteError.Network -> GetTileError.Network(error.cause)
        is RemoteError.Server -> GetTileError.Server(error.type, error.cause)
        is RemoteError.Api -> GetTileError.ApiAuth(error.response)
    }
