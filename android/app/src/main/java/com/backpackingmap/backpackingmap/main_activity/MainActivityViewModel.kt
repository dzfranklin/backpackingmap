package com.backpackingmap.backpackingmap.main_activity

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.backpackingmap.backpackingmap.map.wmts.*
import com.backpackingmap.backpackingmap.map.wmts.os.OsServiceConfig
import com.backpackingmap.backpackingmap.repo.Repo
import timber.log.Timber

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val repo: Repo? = Repo.fromApplication(application)

    val mapService = OsServiceConfig()
    val mapLayerConfigs = arrayOf(mapService.layers.last())

    suspend fun getTile(
        service: WmtsServiceConfig,
        layer: WmtsLayerConfig,
        set: WmtsTileMatrixSetConfig,
        matrix: WmtsTileMatrixConfig,
        position: WmtsTilePosition,
    ): Bitmap = repo!!.getTile(service, layer, set, matrix, position).mapLeft{
        Timber.w("Got error instead of tile: %s", it)
        null
    }.orNull()!!
}