package com.backpackingmap.backpackingmap.repo

import com.mapbox.mapboxsdk.camera.CameraPosition

class Repo {
    val defaultZoom = 12.0

    var _pos: CameraPosition? = null

    suspend fun mapPosition() : CameraPosition? {
        // TODO
        return _pos
    }

    suspend fun setMapPosition(new: CameraPosition) {
        // TODO
        _pos = new
    }
}