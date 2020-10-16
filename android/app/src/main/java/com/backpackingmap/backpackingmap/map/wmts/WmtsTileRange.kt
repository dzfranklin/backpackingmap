package com.backpackingmap.backpackingmap.map.wmts

data class WmtsTileRange(
    val minColOverageInCrs: Double,
    val minRowOverageInCrs: Double,
    val minColInclusive: Int,
    val maxColInclusive: Int,
    val minRowInclusive: Int,
    val maxRowInclusive: Int
)