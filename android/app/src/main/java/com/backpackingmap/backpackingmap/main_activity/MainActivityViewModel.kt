package com.backpackingmap.backpackingmap.main_activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.backpackingmap.backpackingmap.map.wmts.os.OsServiceConfig
import com.backpackingmap.backpackingmap.repo.Repo

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val repo: Repo? = Repo.fromContext(application)

    private val mapService = OsServiceConfig()
    val explorerLayerConfig = mapService.layers.last()
}