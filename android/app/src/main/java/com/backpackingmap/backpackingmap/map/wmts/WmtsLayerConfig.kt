package com.backpackingmap.backpackingmap.map.wmts

data class WmtsLayerConfig(
    val service: WmtsServiceConfig,

    val identifier: String,

    val title: String,

    // TODO
    // val icon: Bitmap
    // val sample: Bitmap

    val wgs84BoundingBox: WmtsBoundingBox,

    // TODO: Consider adding tile format

    // TODO: Consider adding dimension
    // "Extra dimensions for a tile and FeatureInfo resource requests"

    val set: WmtsTileMatrixSetConfig,
    val matrices: Map<WmtsTileMatrixConfig, WmtsTileMatrixLimits>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WmtsLayerConfig

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}
