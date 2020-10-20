package com.backpackingmap.backpackingmap.map

data class Meter(private val meters: Double) {
    operator fun times(other: Meter): Meter = Meter(meters * other.meters)
    operator fun times(other: Double): Meter = Meter(meters * other)
}
