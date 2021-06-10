package com.backpackingmap.backpackingmap.model

import java.util.*

data class TripId(val id: UUID) {
    companion object {
        fun generate() =
            TripId(UUID.randomUUID())
    }
}