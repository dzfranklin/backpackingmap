package com.backpackingmap.backpackingmap

import kotlin.math.round

inline class Pixel(val value: Int) {
    fun toFloat() = value.toFloat()
    fun toDouble() = value.toDouble()

    operator fun unaryMinus() = Pixel(-value)
    operator fun times(other: MetersPerPixel) = Meter(other.value * value)
    operator fun times(other: Int) = Pixel(value * other)
    operator fun minus(other: Pixel) = Pixel(value - other.value)
    operator fun plus(other: Pixel) = Pixel(value + other.value)
    operator fun div(other: Int) = Pixel(value / other)
    operator fun div(other: Double) = Pixel(round(value / other).toInt())
}

fun Double.asPixel() = Pixel(round(this).toInt())
fun Float.asPixel() = this.toDouble().asPixel()
fun Int.asPixel() = Pixel(this)