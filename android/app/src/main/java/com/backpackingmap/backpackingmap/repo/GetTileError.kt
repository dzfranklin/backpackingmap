package com.backpackingmap.backpackingmap.repo

sealed class GetTileError {
    data class Remote(val cause: RemoteError<Nothing>): GetTileError()

    // TODO: Add errrors for caching when that's added
}
