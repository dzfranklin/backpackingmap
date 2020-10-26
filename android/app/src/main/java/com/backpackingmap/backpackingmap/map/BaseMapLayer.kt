package com.backpackingmap.backpackingmap.map

import org.locationtech.proj4j.CoordinateReferenceSystem

abstract class BaseMapLayer : MapLayer() {
    abstract class Builder<out T : BaseMapLayer> : MapLayer.Builder<T>() {
        /** The base coordinate reference system to project the entire map in */
        abstract val baseCrs: CoordinateReferenceSystem
    }
}