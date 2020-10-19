package com.backpackingmap.backpackingmap.net.tile

import com.backpackingmap.backpackingmap.map.wmts.WmtsLayerConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsServiceConfig
import com.backpackingmap.backpackingmap.map.wmts.WmtsTileMatrixConfig

data class GetTileRequest(
    val serviceIdentifier: String,
    val layerIdentifier: String,
    val setIdentifier: String,
    val matrixIdentifier: String,
    val position: TileRequestPosition
) {
    data class Builder(
        val serviceIdentifier: String,
        val layerIdentifier: String,
        val setIdentifier: String,
        val matrixIdentifier: String
    ) {
        fun build(row: Int, col: Int) =
            GetTileRequest(
                serviceIdentifier,
                layerIdentifier,
                setIdentifier,
                matrixIdentifier,
                TileRequestPosition(row, col)
            )

        companion object {
            fun from(
                service: WmtsServiceConfig,
                layer: WmtsLayerConfig,
                matrix: WmtsTileMatrixConfig
            ) = Builder(
                service.identifier,
                layer.identifier,
                layer.set.identifier,
                matrix.identifier
            )

        }
    }
}