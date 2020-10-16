package com.backpackingmap.backpackingmap.map.wmts

interface WmtsServiceConfig {
    val identifier: String
    val layers: Array<WmtsLayerConfig>
}