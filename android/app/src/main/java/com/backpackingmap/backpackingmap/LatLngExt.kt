package com.backpackingmap.backpackingmap

import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng

fun LatLng.toPoint(): Point =
    Point.fromLngLat(longitude, latitude)

fun LatLng.toFeature(): Feature =
    Feature.fromGeometry(toPoint())

fun LatLng.copy(lat: Double? = null, lng: Double? = null): LatLng =
    LatLng(lat ?: this.latitude, lng ?: this.longitude)