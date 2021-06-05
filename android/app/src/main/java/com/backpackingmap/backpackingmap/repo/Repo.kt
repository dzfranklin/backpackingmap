package com.backpackingmap.backpackingmap.repo

import com.mapbox.mapboxsdk.camera.CameraPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class Repo {
    val defaultZoom = 12.0

    var _pos: CameraPosition? = null

    suspend fun mapPosition(): CameraPosition? {
        // TODO
        return _pos
    }

    suspend fun setMapPosition(new: CameraPosition) {
        // TODO
        _pos = new
    }

    var _cm = MutableStateFlow(CoordinateMode.LatLng)

    fun coordinateMode(): Flow<CoordinateMode> {
        // TODO
        return _cm
    }

    suspend fun setCoordinateMode(new: CoordinateMode) {
        // TODO
        _cm.value = new
    }
}