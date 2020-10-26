package com.backpackingmap.backpackingmap.map

import com.backpackingmap.backpackingmap.NaiveCoordinate
import org.locationtech.proj4j.CoordinateReferenceSystem

data class MapState(
    val baseCrs: CoordinateReferenceSystem,
    // NOTE: Center is a naive coordinate to enforce the fact that everything must be in baseCrs
    val center: NaiveCoordinate,
    val zoom: ZoomLevel,
    val size: MapSize,
)
