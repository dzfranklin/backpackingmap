package com.backpackingmap.backpackingmap.main_activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.backpackingmap.backpackingmap.map.wmts.os.OsServiceConfig
import com.backpackingmap.backpackingmap.repo.Repo

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val repo: Repo? = Repo.fromApplication(application)

    val mapService = OsServiceConfig()
    val mapLayerConfigs = arrayOf(mapService.layers.last())
}