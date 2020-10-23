package com.backpackingmap.backpackingmap.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import arrow.core.Either
import com.backpackingmap.backpackingmap.map.ZoomLevel
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTileMatrixConfig
import com.backpackingmap.backpackingmap.net.ApiService
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

typealias GetTileResponse = Either<GetTileError, Bitmap>

class TileRepo(
    override val coroutineContext: CoroutineContext,
    private val accessTokenCache: AccessTokenCache,
    private val api: ApiService,
    private val size: Int,
) : CoroutineScope {
    private val cache = object : LruCache<GetTileRequest, GetTileResponse>(size) {
        override fun sizeOf(key: GetTileRequest, value: GetTileResponse) = when (value) {
            is Either.Right -> value.b.byteCount / 1024
            is Either.Left -> 0
        }
    }

    fun getCached(key: GetTileRequest): GetTileResponse? = cache.get(key)

    private val requesting = HashSet<GetTileRequest>()

    /**
     * If you make multiple requests in short succession and check the cache before each only one
     * request will be made.
     */
    fun requestCaching(requests: Collection<GetTileRequest>, onCacheTile: suspend () -> Unit) {
        launch {
            for (request in requests) {
                launch {
                    requestCaching(request, onCacheTile)
                }
            }
        }
    }

    private suspend fun requestCaching(request: GetTileRequest, onCacheTile: suspend () -> Unit) {
        // TODO: Don't request tiles that don't exist
        if (!requesting.contains(request)) {
            requesting.add(request)

            val result = makeRemoteRequestForBody(accessTokenCache) { token ->
                api.getTile(token, request)
            }
                .map {
                    BitmapFactory.decodeStream(it.byteStream())
                }
                .mapLeft { GetTileError.Remote(it) }

            cache.put(request, result)
            requesting.remove(request)
            onCacheTile()
        }
    }

    data class ClosestMatrixData(
        val targetMetersPerPixel: Float,
        val metersPerPixel: Float,
        val matrix: WmtsTileMatrixConfig,
    )

    fun findClosestMatrix(layer: WmtsLayerConfig, zoom: ZoomLevel): ClosestMatrixData? {
        val targetMetersPerPixel = zoom.metersPerPixel

        var closestMetersPerPixel: Float? = null
        var closestMatrix: WmtsTileMatrixConfig? = null

        for (matrix in layer.matrices.keys) {
            val metersPerPixel = layer.set.metersPerPixel(matrix).toFloat()

            if (closestMatrix == null || closestMetersPerPixel == null) {
                closestMetersPerPixel = metersPerPixel
                closestMatrix = matrix
                continue
            }

            if (abs(targetMetersPerPixel - metersPerPixel) < abs(targetMetersPerPixel - closestMetersPerPixel)) {
                closestMetersPerPixel = metersPerPixel
                closestMatrix = matrix
            }
        }

        return if (closestMetersPerPixel != null && closestMatrix != null) {
            ClosestMatrixData(targetMetersPerPixel, closestMetersPerPixel, closestMatrix)
        } else {
            null
        }
    }
}