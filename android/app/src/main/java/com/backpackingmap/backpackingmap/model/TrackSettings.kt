package com.backpackingmap.backpackingmap.model

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class TrackSettings constructor(
    val interval: Duration
)