package com.backpackingmap.backpackingmap.map

data class Pixel(private val pixels: Int) {
    fun toInt() = pixels

    fun toDouble() = pixels.toDouble()

    fun toFloat() = pixels.toFloat()
}
