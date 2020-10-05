package com.backpackingmap.backpackingmap.map_activity

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.backpackingmap.backpackingmap.net.tile.TileType
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: Repo? = Repo.fromApplication(application)

    val tile = MutableLiveData<Bitmap>()

    init {
        viewModelScope.launch {
            repo
                ?.getTile(TileType.Explorer, 2025, 773)
                ?.map {
                    tile.value = it
                }
                ?.mapLeft {
                    throw Exception(it.toString())
                }
        }
    }
}