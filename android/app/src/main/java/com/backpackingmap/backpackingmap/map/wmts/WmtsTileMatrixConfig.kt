package com.backpackingmap.backpackingmap.map.wmts

import com.backpackingmap.backpackingmap.map.NaiveCoordinate
import com.backpackingmap.backpackingmap.map.Pixel

data class WmtsTileMatrixConfig(
    val identifier: String,

    val scaleDenominator: Double,

    val topLeftCorner: NaiveCoordinate,

    val tileWidth: Pixel,
    val tileHeight: Pixel,

    val matrixWidthInTiles: Int,
    val matrixHeightInTiles: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WmtsTileMatrixConfig

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}
