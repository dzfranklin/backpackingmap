package com.backpackingmap.backpackingmap.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import arrow.core.Either
import com.backpackingmap.backpackingmap.net.ApiService
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

typealias GetTileResponse = Either<GetTileError, Bitmap>

class TileRepo(
    override val coroutineContext: CoroutineContext,
    private val accessTokenCache: AccessTokenCache,
    private val api: ApiService,
) : CoroutineScope {
    private val appMaxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val size = appMaxMemory / 4

    private val cache = object : LruCache<GetTileRequest, GetTileResponse>(size) {
        override fun sizeOf(key: GetTileRequest, value: GetTileResponse) = when (value) {
            is Either.Right -> value.b.byteCount / 1024
            is Either.Left -> 0
        }
    }

    fun getCached(key: GetTileRequest): GetTileResponse? = cache.get(key)

    fun requestCaching(request: GetTileRequest, onCached: () -> Unit) {
        launch {
            val result = makeRemoteRequestForBody(accessTokenCache) { token ->
                api.getTile(token, request)
            }
                .map {
                    BitmapFactory.decodeStream(it.byteStream())
                }
                .mapLeft { GetTileError.Remote(it) }

            cache.put(request, result)

            onCached()
        }
    }
}