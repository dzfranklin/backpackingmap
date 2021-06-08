package com.backpackingmap.backpackingmap.model.prettify_drawn_line

import com.backpackingmap.backpackingmap.model.Vec2F
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Given a curve defined by a [list] of line segments find a similar curve with fewer points.
 *
 * The Ramer–Douglas–Peucker algorithm (RDP) is an algorithm for reducing the number of points in a
 * curve that is approximated by a series of points.
 */
fun rdpReduce(list: List<Vec2F>, epsilon: Float): List<Vec2F> {
    val resultList: MutableList<Vec2F> = mutableListOf()
    rdpReduce(list, 0, list.size, epsilon, resultList)
    return resultList
}

private fun rdpReduce(
    list: List<Vec2F>,
    s: Int,
    e: Int,
    epsilon: Float,
    resultList: MutableList<Vec2F>
) {
    // Find the point with the maximum distance
    var dMax = 0f
    var index = 0
    val endIdx = e - 1
    val start = list[s]
    val end = list[endIdx]
    for (i in s + 1 until endIdx) {
        val point = list[i]
        val d = perpendicularDistance(point, start, end)
        if (d > dMax) {
            index = i
            dMax = d
        }
    }

    // If max distance is greater than epsilon, recursively simplify
    if (dMax > epsilon) {
        // Recursive call
        rdpReduce(list, s, index, epsilon, resultList)
        rdpReduce(list, index, e, epsilon, resultList)
    } else {
        if (endIdx - s > 0) {
            resultList.add(list[s])
            resultList.add(list[endIdx])
        } else {
            resultList.add(list[s])
        }
    }
}

private fun distanceToSegmentSquared(
    point: Vec2F,
    start: Vec2F,
    end: Vec2F
): Float {
    val l2 = distanceBetweenPoints(start, end)
    if (l2 == 0f) return distanceBetweenPoints(point, start)

    val t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / l2
    if (t < 0) return distanceBetweenPoints(point, start)

    return if (t > 1) distanceBetweenPoints(point, end) else {
        distanceBetweenPoints(
            point,
            Vec2F(
                start.x + t * (end.x - start.x),
                start.y + t * (end.y - start.y)
            )
        )
    }
}

private fun distanceBetweenPoints(v: Vec2F, w: Vec2F): Float {
    return (v.x - w.x).pow(2f) + (v.y - w.y).pow(2f)
}

private fun perpendicularDistance(point: Vec2F, start: Vec2F, end: Vec2F) =
    sqrt(distanceToSegmentSquared(point, start, end))
