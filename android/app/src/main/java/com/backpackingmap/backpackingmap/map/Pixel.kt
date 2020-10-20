package com.backpackingmap.backpackingmap.map

data class Pixel(private val pixels: Int) {
    fun toInt() = pixels
    fun toFloat() = pixels.toFloat()
}
