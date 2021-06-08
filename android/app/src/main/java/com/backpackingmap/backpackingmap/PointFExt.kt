package com.backpackingmap.backpackingmap

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import kotlin.math.sqrt

fun PointF.toOffset() =
    Offset(x, y)

fun PointF.distanceTo(other: PointF): Float =
    sqrt((other.x - x).pow(2) + (other.y - y).pow(2))
