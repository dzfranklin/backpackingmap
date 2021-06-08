package com.backpackingmap.backpackingmap.model.prettify_drawn_line

import com.backpackingmap.backpackingmap.model.Vec2F

class CubicBezier(p0: Vec2F, p1: Vec2F, p2: Vec2F, p3: Vec2F) : Bezier(listOf(p0, p1, p2, p3)) {
    val p0 get() = controls[0]
    val p1 get() = controls[1]
    val p2 get() = controls[2]
    val p3 get() = controls[3]
}
