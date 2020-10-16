package com.backpackingmap.backpackingmap.net.tile

data class GetTileRequest(
    val serviceIdentifier: String,
    val layerIdentifier: String,
    val setIdentifier: String,
    val matrixIdentifier: String,
    val position: TileRequestPosition
)