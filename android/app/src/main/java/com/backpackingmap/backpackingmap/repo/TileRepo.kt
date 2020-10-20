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
    private val size: Int
) : CoroutineScope {
    private val cache = object : LruCache<GetTileRequest, GetTileResponse>(size) {
        override fun sizeOf(key: GetTileRequest, value: GetTileResponse) = when (value) {
            is Either.Right -> value.b.byteCount / 1024
            is Either.Left -> 0
        }
    }

    fun getCached(key: GetTileRequest): GetTileResponse? = cache.get(key)

    private val toNotify = HashMap<Pair<Any, GetTileRequest>, () -> Unit>()

    /**
     * A call with the same requestIdentifier and request to a previous call made before the
     * onCached provided in the previous call was called will cause the new onCached to replace
     * the previous onCached such that when the previously started request is completed the new
     * onCached will be called, and no other onCached will be called.
     */
    fun requestCaching(requesterIdentifier: Any, request: GetTileRequest, onCached: () -> Unit) {
        // TODO: Don't request tiles that don't exist
        launch {
            val identifier = requesterIdentifier to request
            val alreadyRequested = toNotify.contains(identifier)
            toNotify[identifier] = onCached

            if (!alreadyRequested) {
                val result = makeRemoteRequestForBody(accessTokenCache) { token ->
                    api.getTile(token, request)
                }
                    .map {
                        BitmapFactory.decodeStream(it.byteStream())
                    }
                    .mapLeft { GetTileError.Remote(it) }

                cache.put(request, result)

                val notifier = toNotify[identifier]
                if (notifier != null) {
                    toNotify.remove(identifier)
                    notifier()
                }
            }
        }
    }
}