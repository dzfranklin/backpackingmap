package com.backpackingmap.backpackingmap.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import arrow.core.Either
import com.backpackingmap.backpackingmap.LIFOQueue
import com.backpackingmap.backpackingmap.MetersPerPixel
import com.backpackingmap.backpackingmap.map.ZoomLevel
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTileMatrixConfig
import com.backpackingmap.backpackingmap.net.ApiService
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

typealias GetTileResponse = Either<GetTileError, Bitmap>

class TileRepo(
    override val coroutineContext: CoroutineContext,
    private val accessTokenCache: AccessTokenCache,
    private val api: ApiService,
    private val size: Int,
) : CoroutineScope {
    private val cache = object : LruCache<GetTileRequest, Bitmap>(size) {
        override fun sizeOf(key: GetTileRequest, value: Bitmap) = value.byteCount / 1024
    }

    private data class Request(
        val request: GetTileRequest,
        val onCached: suspend (GetTileRequest, Bitmap) -> Unit,
    )

    private val unsentRequests = LIFOQueue<Request>()

    init {
        launch {
            (0 until MAX_IN_FLIGHT_REQUESTS)
                .map { createRequester() }
        }
    }

    private fun CoroutineScope.createRequester() {
        launch {
            unsentRequests.collect { request ->
                val result = makeRemoteRequestForBody(accessTokenCache) { token ->
                    api.getTile(token, request.request)
                }

                if (result is Either.Right) {
                    val bitmap = BitmapFactory.decodeStream(result.b.byteStream())
                    cache.put(request.request, bitmap)
                    request.onCached(request.request, bitmap)
                } else {
                    Timber.w("Failed to get tile: %s", result)
                    // TODO: Retry logic.
                }
            }
        }
    }

    fun getCached(key: GetTileRequest): Bitmap? = cache.get(key)

    fun requestCaching(
        request: GetTileRequest,
        onCached: suspend (GetTileRequest, Bitmap) -> Unit,
    ) {
        unsentRequests.enqueue(Request(request, onCached))
    }

    data class ClosestMatrixData(
        val targetMetersPerPixel: MetersPerPixel,
        val metersPerPixel: MetersPerPixel,
        val matrix: WmtsTileMatrixConfig,
    )

    fun findClosestMatrix(layer: WmtsLayerConfig, zoom: ZoomLevel): ClosestMatrixData? {
        val target = zoom.level

        var closest: MetersPerPixel? = null
        var closestMatrix: WmtsTileMatrixConfig? = null

        for (matrix in layer.matrices.keys) {
            val forThisMatrix = layer.set.metersPerPixel(matrix)

            if (closestMatrix == null || closest == null) {
                closest = forThisMatrix
                closestMatrix = matrix
                continue
            }

            if (abs(target.value - forThisMatrix.value) < abs(target.value - closest.value)) {
                closest = forThisMatrix
                closestMatrix = matrix
            }
        }

        return if (closest != null && closestMatrix != null) {
            ClosestMatrixData(target, closest, closestMatrix)
        } else {
            null
        }
    }

    companion object {
        private const val MAX_IN_FLIGHT_REQUESTS = 20
    }
}