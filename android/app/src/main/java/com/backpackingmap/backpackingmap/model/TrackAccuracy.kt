package com.backpackingmap.backpackingmap.model

import kotlin.time.Duration

data class TrackAccuracy(
    gpsInterval: Duration? = null,
    deadReckonInterval: Duration? = null
) {
    companion object {
        val prese
    }
}
