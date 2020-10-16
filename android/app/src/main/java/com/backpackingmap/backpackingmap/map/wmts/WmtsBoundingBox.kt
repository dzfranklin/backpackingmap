package com.backpackingmap.backpackingmap.map.wmts

import com.backpackingmap.backpackingmap.map.NaiveCoordinate

// LatLng contains a CRS, so omitted from BB
data class WmtsBoundingBox(
    // Coordinates of bounding box corner at which the values of latitude and longitude normally are
    // the algebraic minima within this bounding box
    // Per OGC 06-121r9, page 59
    val lowerCorner: NaiveCoordinate,

    // Coordinates of bounding box corner at which the values of latitude and longitude normally are
    // the algebraic maximums within this bounding box
    // Per OGC 06-121r9, page 59
    val upperCorner: NaiveCoordinate
)
