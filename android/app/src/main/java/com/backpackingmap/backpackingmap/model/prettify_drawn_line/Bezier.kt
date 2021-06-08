package com.backpackingmap.backpackingmap.model.prettify_drawn_line

import com.backpackingmap.backpackingmap.model.Vec2F

open class Bezier(
    /** Control points */
    val controls: List<Vec2F>
) {
    /** Evaluate a Bezier curve at the parametric value [t] */
    fun evaluateAt(t: Float): Vec2F {
        val degree = controls.size - 1
        val temp = MutableList(controls.size) { controls[it] }
        for (i in 1..degree) {
            for (j in 0..degree - i) {
                temp[j].x = (1f - t) * temp[j].x + t * temp[j + 1].x
                temp[j].y = (1f - t) * temp[j].y + t * temp[j + 1].y
            }
        }
        return temp[0]
    }

    override fun hashCode() = controls.hashCode()

    override fun equals(other: Any?) = this.hashCode() == other.hashCode()

    override fun toString() =
        controls.withIndex().joinToString(", ", "CubicBezier(", ")") { (idx, vec) -> "p$idx=$vec" }
}
