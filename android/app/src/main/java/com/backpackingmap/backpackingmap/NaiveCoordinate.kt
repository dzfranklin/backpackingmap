package com.backpackingmap.backpackingmap

data class NaiveCoordinate(
    val x: Double,
    val y: Double,
)

fun Coordinate.asNaive() = NaiveCoordinate(x, y)