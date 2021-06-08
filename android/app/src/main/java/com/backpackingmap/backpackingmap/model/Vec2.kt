package com.backpackingmap.backpackingmap.model

import kotlin.math.pow
import kotlin.math.sqrt

data class Vec2F(
    var x: Float,
    var y: Float,
) {
    fun distanceTo(other: Vec2F): Float =
        sqrt((other.x - x).pow(2) + (other.y - y).pow(2))

    fun distanceToSquared(other: Vec2F): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return dx.pow(2) + dy.pow(2)
    }

    fun equalsOrClose(other: Vec2F): Boolean =
        distanceToSquared(other) < EPSILON

    fun normalized(): Vec2F {
        val dist = sqrt(x.pow(2) + y.pow(2))
        val normX = this.x / dist
        val normY = this.y / dist
        return Vec2F(normX, normY)
    }

    operator fun minus(other: Vec2F): Vec2F =
        Vec2F(this.x - other.x, this.y - other.y)

    operator fun plus(other: Vec2F): Vec2F =
        Vec2F(this.x + other.x, this.y + other.y)

    operator fun div(other: Float): Vec2F =
        Vec2F(x / other, y / other)

    operator fun times(other: Float): Vec2F =
        Vec2F(x * other, y * other)

    operator fun unaryMinus(): Vec2F =
        Vec2F(-x, -y)

    /** Dot product */
    operator fun times(other: Vec2F): Float =
        this.x * other.x + this.y * other.y

    fun length(): Float =
        sqrt(x.pow(2) + y.pow(2))

    fun squaredLength(): Float =
        x.pow(2) + y.pow(2)

    override fun toString() =
        "($x, $y)"

    companion object {
        fun zero() = Vec2F(0f, 0f)
    }
}

const val EPSILON = 1.2e-12f
