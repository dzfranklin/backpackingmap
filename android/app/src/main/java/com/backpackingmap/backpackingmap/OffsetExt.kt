package com.backpackingmap.backpackingmap

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset

fun Offset.toPointF() =
    PointF(x, y)