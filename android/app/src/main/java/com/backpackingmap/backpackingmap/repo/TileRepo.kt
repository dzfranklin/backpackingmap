package com.backpackingmap.backpackingmap.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTileMatrixConfig
import com.backpackingmap.backpackingmap.net.ApiService
import com.backpackingmap.backpackingmap.net.tile.GetTileRequest
import com.backpackingmap.backpackingmap.net.tile.TileRequestPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class TileRepo(
    override val coroutineContext: CoroutineContext,
    private val accessTokenCache: AccessTokenCache,
    private val api: ApiService,
) : CoroutineScope {
    private val appMaxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val size = appMaxMemory / 4

    private val cache = object : LruCache<Int, Bitmap>(size) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getCached(
        serviceLayerMatrixIdentifier: Int,
        row: Int,
        col: Int,
    ): Bitmap? =
        cache.get(tileIdentifier(serviceLayerMatrixIdentifier, row, col))

    fun requestCaching(
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        matrix: WmtsTileMatrixConfig,
        row: Int,
        col: Int,
        onCached: () -> Unit
    ) {
        launch {
            val request =
                GetTileRequest(
                    serviceIdentifier = service.identifier,
                    layerIdentifier = layer.identifier,
                    setIdentifier = layer.set.identifier,
                    matrixIdentifier = matrix.identifier,
                    position = TileRequestPosition(row = row, col = col)
                )
            val serviceLayerMatrixIdentifier = serviceLayerMatrixIdentifier(
                request.serviceIdentifier,
                request.layerIdentifier,
                request.matrixIdentifier
            )
            val identifier = tileIdentifier(
                serviceLayerMatrixIdentifier,
                request.position.row,
                request.position.col
            )

            makeRemoteRequestForBody(accessTokenCache) { token ->
                api.getTile(token, request)
            }
                .map {
                    val bitmap = BitmapFactory.decodeStream(it.byteStream())
                    cache.put(identifier, bitmap)
                    onCached()
                }
                .mapLeft {
                    Timber.w("Error fetching tile")
                }
        }
    }

    fun tileIdentifier(serviceLayerMatrixIdentifier: Int, row: Int, col: Int) =
        // NOTE: 31 is an arbitrary prime
        serviceLayerMatrixIdentifier * 31 + row * 31 + col

    fun serviceLayerMatrixIdentifier(
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        matrix: WmtsTileMatrixConfig,
    ): Int = serviceLayerMatrixIdentifier(service.identifier, layer.identifier, matrix.identifier)

    private fun serviceLayerMatrixIdentifier(
        serviceIdentifier: String,
        layerIdentifier: String,
        matrixIdentifier: String,
    ): Int {
        // NOTE: This is how IntelliJ auto-generates hashCode
        val a = serviceIdentifier.hashCode()
        val b = layerIdentifier.hashCode()
        val c = matrixIdentifier.hashCode()
        // NOTE: 31 is an arbitrary prime
        return a * 31 + b * 31 + c
    }
}