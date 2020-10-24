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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
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

    private data class Request(val request: GetTileRequest, val onCached: suspend () -> Unit)

    private val unsentRequests = ArrayDeque<Request>()
    private val requesting = HashSet<GetTileRequest>()

    init {
        launch {
            val inFlightRequests = AtomicInteger(0)

            while (true) {
                if (inFlightRequests.get() < MAX_IN_FLIGHT_REQUESTS) {
                    val request = unsentRequests.removeFirstOrNull()
                    if (request != null) {
                        inFlightRequests.incrementAndGet()
                        requesting.add(request.request)
                        launch {
                            val result = makeRemoteRequestForBody(accessTokenCache) { token ->
                                api.getTile(token, request.request)
                            }

                            inFlightRequests.decrementAndGet()

                            if (result is Either.Right) {
                                val bitmap = BitmapFactory.decodeStream(result.b.byteStream())
                                cache.put(request.request, bitmap)
                                requesting.remove(request.request)
                                request.onCached()
                            } else {
                                requesting.remove(request.request)
                            }
                        }
                    }
                }

                delay(1)
            }
        }
    }

    fun getCached(key: GetTileRequest): Bitmap? = cache.get(key)

    /**
     * If you make multiple requests in short succession and check the cache before each only one
     * request will be made.
     */
    fun requestCaching(request: GetTileRequest, onCached: suspend () -> Unit) {
        unsentRequests.addFirst(Request(request, onCached))
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

    companion object {
        private const val MAX_IN_FLIGHT_REQUESTS = 20
    }
}