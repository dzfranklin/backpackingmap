package com.backpackingmap.backpackingmap

inline class MetersPerPixel(val value: Double) {
    fun inverse() = PixelsPerMeter(1.0 / value)

    operator fun times(other: MetersPerPixel) = MetersPerPixel(value * other.value)
    operator fun times(other: Double) = MetersPerPixel(value * other)
    operator fun div(other: MetersPerPixel) = value / other.value
}
