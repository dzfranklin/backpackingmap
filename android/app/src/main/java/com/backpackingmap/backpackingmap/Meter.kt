package com.backpackingmap.backpackingmap

import kotlin.math.round

inline class Meter(val value: Double) {
    fun toDouble() = value

    operator fun div(other: Pixel) = MetersPerPixel(value / other.value)
    operator fun times(other: PixelsPerMeter) = Pixel(round(value * other.value).toInt())
    operator fun times(other: Double) = Meter(value * other)
    operator fun div(other: Double) = Meter(value / other)
    operator fun minus(other: Meter) = Meter(value - other.value)
    operator fun plus(other: Meter) = Meter(value + other.value)
}

fun Double.asMeters() = Meter(this)
operator fun Double.times(other: Meter) = Meter(this * other.value)