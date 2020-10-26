package com.backpackingmap.backpackingmap.map.wmts

data class WmtsTileRange(
    val minColOverage: Double,
    val minRowOverage: Double,
    val minColInclusive: Int,
    val maxColInclusive: Int,
    val minRowInclusive: Int,
    val maxRowInclusive: Int
)