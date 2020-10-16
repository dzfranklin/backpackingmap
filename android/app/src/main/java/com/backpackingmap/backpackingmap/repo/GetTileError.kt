package com.backpackingmap.backpackingmap.repo

import com.backpackingmap.backpackingmap.net.ResponseErrorWithMessage

sealed class GetTileError {
    data class Network(val cause: Throwable) : GetTileError()

    data class Server(val type: String, val cause: Throwable) : GetTileError()

    data class ApiAuth(val response: ResponseErrorWithMessage) : GetTileError()

    // TODO: Add errrors for caching when that's added
}
