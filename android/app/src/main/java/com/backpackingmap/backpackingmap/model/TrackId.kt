package com.backpackingmap.backpackingmap.model

import java.util.*

data class TrackId(val id: UUID) {
    companion object {
        fun generate() =
            TrackId(UUID.randomUUID())
    }
}