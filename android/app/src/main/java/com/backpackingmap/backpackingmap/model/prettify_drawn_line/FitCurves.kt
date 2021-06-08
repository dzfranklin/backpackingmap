package com.backpackingmap.backpackingmap.model.prettify_drawn_line

import com.backpackingmap.backpackingmap.model.Vec2F
import kotlin.math.pow

/** Fit bezier curves to a set of points.
 *
 * Note: [error] is not squared error
 */
fun fitCurves(points: List<Vec2F>, error: Float): List<CubicBezier> {
    val tHat1 = computeLeftTangent(points, 0)
    val tHat2 = computeRightTangent(points, points.size - 1)
    val curves = mutableListOf<CubicBezier>()
    fitCubic(points, 0, points.size - 1, tHat1, tHat2, error.pow(2), curves)
    return curves
}

/** Fit Bezier curves to a (sub)set of digitized points
 *
 * Args:
 * - [d] Array of digitized points
 * - [first], [last] Indices of first and last pts in region
 * - [tHat1], [tHat2] Unit tangent vectors at endpoints
 * - [error] User-defined error squared
 * */
private fun fitCubic(
    d: List<Vec2F>,
    first: Int,
    last: Int,
    tHat1: Vec2F,
    tHat2: Vec2F,
    error: Float,
    curvesOut: MutableCollection<CubicBezier>
) {
    // Error below which you try iterating
    val iterationError = error * 4.0 // (original had comment "fixed issue 23")
    val maxIterations = 4

    // Number of points in subset
    val nPts = last - first + 1

    if (nPts == 2) {
        // Use heuristic if region only has two points in it

        val dist = d[last].distanceTo(d[first]) / 3f
        val p0 = d[first]
        val p3 = d[last]
        val p1 = (tHat1 * dist) + p0
        val p2 = (tHat2 * dist) + p3
        curvesOut.add(CubicBezier(p0, p1, p2, p3))
        return
    }

    // Parameterize points, and attempt to fit curve
    var u = chordLengthParameterize(d, first, last)
    var bezCurve = generateBezier(d, first, last, u, tHat1, tHat2)

    // Find max deviation of points to fitted curve
    var maxResult = computeMaxError(d, first, last, bezCurve, u)
    var maxError = maxResult.maxError
    var splitPoint = maxResult.splitPoint
    if (maxError < error) {
        curvesOut.add(bezCurve)
    }

    //  If error not too large, try some reparameterization and iteration
    if (maxError < iterationError) {
        for (i in 0 until maxIterations) {
            // Improved parameter values
            val uPrime = reparameterize(d, first, last, u, bezCurve)
            bezCurve = generateBezier(d, first, last, uPrime, tHat1, tHat2)
            maxResult = computeMaxError(d, first, last, bezCurve, uPrime)
            maxError = maxResult.maxError
            splitPoint = maxResult.splitPoint
            if (maxError < error) {
                curvesOut.add(bezCurve)
                return
            }
            u = uPrime
        }
    }

    // Fitting failed -- split at max error point and fit recursively
    val tHatCenter = computeCenterTangent(d, splitPoint)
    fitCubic(d, first, splitPoint, tHat1, tHatCenter, error, curvesOut)
    fitCubic(d, splitPoint, last, -tHatCenter, tHat2, error, curvesOut)
}

/** Given set of points and their parameterization, try to find a better parameterization. */
private fun reparameterize(
    d: List<Vec2F>,
    first: Int,
    last: Int,
    u: List<Float>,
    bezCurve: CubicBezier
): List<Float> {
    val uPrime = MutableList(u.size) { 0f }
    for (i in first..last) {
        uPrime[i - first] = newtonRaphsonRootFind(bezCurve, d[i], u[i - first])
    }
    return uPrime
}

/** Use Newton-Raphson iteration to find better root given a current fitted curve [q],
 * a digitized point [p], and a parameter value for p [u] */
private fun newtonRaphsonRootFind(q: CubicBezier, p: Vec2F, u: Float): Float {
    val qC = q.controls
    val q1C = MutableList(3) { Vec2F.zero() } // Q'
    val q2C = MutableList(2) { Vec2F.zero() } // Q''

    // Compute Q(u)
    val qAtU = q.evaluateAt(u)

    // Generate control vertices for Q'
    for (i in 0..2) {
        q1C[i].x = (qC[i + 1].x - qC[i].x) * 3f
        q1C[i].y = (qC[i + 1].y - qC[i].y) * 3f
    }
    val q1 = Bezier(q1C)

    // Generate control vertices for Q''
    for (i in 0..1) {
        q2C[i].x = (q1C[i + 1].x - q1C[i].x) * 2f
        q2C[i].y = (q1C[i + 1].y - q1C[i].y) * 2f
    }
    val q2 = Bezier(q2C)

    // Compute Q'(u) and Q''(u)
    val q1AtU = q1.evaluateAt(u)
    val q2AtU = q2.evaluateAt(u)

    // f(u)/f'(u)
    val numerator = (qAtU.x - p.x) * q1AtU.x + (qAtU.y - p.y) * q1AtU.y
    val denominator =
        q1AtU.x * q1AtU.x + q1AtU.y * q1AtU.y + (qAtU.x - p.x) * q2AtU.x + (qAtU.y - p.y) * q2AtU.y
    if (denominator == 0f) return u

    // u = u - f(u)/f'(u)
    return u - (numerator / denominator)
}

data class MaxResult(
    val maxError: Float,
    val splitPoint: Int,
)

/** Find the maximum squared distance of digitized points to the fitted curve [bezCurve]
 * given the parameterization of points [u]. */
private fun computeMaxError(
    d: List<Vec2F>,
    first: Int,
    last: Int,
    bezCurve: CubicBezier,
    u: List<Float>,
): MaxResult {
    var splitPoint = (last - first + 1) / 2
    var maxDist = 0f // Maximum error
    for (i in first + 1 until last) {
        val p = bezCurve.evaluateAt(u[i - first])
        val v = p - d[i]
        val dist = v.squaredLength()
        if (dist >= maxDist) {
            maxDist = dist
            splitPoint = i
        }
    }
    return MaxResult(maxDist, splitPoint)
}

/** Use least-squares method to find Bezier control points for region.
 * Args:
 * [d] Array of digitized points
 * [first], [last] Indices defining region
 * [uPrime] Parameter values for region
 * [tHat1], [tHat2] Unit tangents at endpoints */
private fun generateBezier(
    d: List<Vec2F>,
    first: Int,
    last: Int,
    uPrime: List<Float>,
    tHat1: Vec2F,
    tHat2: Vec2F
): CubicBezier {
    val nPts = last - first + 1

    // Precomputed rhs for eqn
    val aMat = mutableListOf<Pair<Vec2F, Vec2F>>()

    // Matrix C
    val cMat = arrayOf(Vec2F.zero(), Vec2F.zero())

    // Matrix X
    val xMat = Vec2F(0f, 0f)

    // Compute the A's
    for (i in 0 until nPts) {
        val v1 = tHat1 * b1(uPrime[i])
        val v2 = tHat2 * b2(uPrime[i])
        aMat.add(v1 to v2)
    }

    // Create the C and X matrices
    for (i in 0 until nPts) {
        val a = aMat[i]
        cMat[0].x += a.first * a.first
        cMat[0].y += a.first * a.second
        cMat[1].x = cMat[0].y
        cMat[1].y += a.second * a.second
        val tmp =
            d[first + i] - (d[first] * b0(uPrime[i]) + d[first] * b1(uPrime[i]) + d[last] * b2(
                uPrime[i]
            ) + d[last] * b3(uPrime[i]))
        xMat.x += aMat[i].first * tmp
        xMat.y += aMat[i].second * tmp
    }

    // Compute the determinants of C and X
    val detC0C1 = cMat[0].x * cMat[1].y - cMat[1].x * cMat[0].y
    val detC0X = cMat[0].x * xMat.y - cMat[1].x * xMat.x
    val detXC1 = xMat.x * cMat[1].y - xMat.y * cMat[0].y

    // Finally, derive alpha values
    val alphaL = if (detC0C1 == 0f) 0f else detXC1 / detC0C1
    val alphaR = if (detC0C1 == 0f) 0f else detC0X / detC0C1


    // If alpha negative, use the Wu/Barsky heuristic (see text)
    //   (if alpha is 0, you get coincident control points that lead to
    //   divide by zero in any subsequent NewtonRaphsonRootFind() call.
    val segLength = d[last].distanceTo(d[first])
    val epsilon = 1.0e-6f * segLength
    if (alphaL < epsilon || alphaR < epsilon) {
        // Fall back on standard (probably inaccurate) formula, and subdivide further if needed.
        val dist = segLength / 3f
        val p0 = d[first]
        val p3 = d[last]
        val p1 = p0 + tHat1 * dist
        val p2 = p3 + tHat2 * dist
        return CubicBezier(p0, p1, p2, p3)
    }

    // First and last control points of the Bezier curve are
    //   positioned exactly at the first and last data points
    //   Control points 1 and 2 are positioned an alpha distance out
    //   on the tangent vectors, left and right, respectively
    val p0 = d[first]
    val p3 = d[last]
    val p1 = p0 + tHat1 * alphaL
    val p2 = p3 + tHat2 * alphaR
    return CubicBezier(p0, p1, p2, p3)
}

/** Assign parameter values to digitized points using relative distances between points. */
private fun chordLengthParameterize(d: List<Vec2F>, first: Int, last: Int): List<Float> {
    val u = MutableList(last - first + 1) { 0f }

    u[0] = 0f

    for (i in first + 1..last) {
        u[i - first] = u[i - first - 1] + d[i].distanceTo(d[i - 1])
    }

    for (i in first + 1..last) {
        u[i - first] = u[i - first] / u[last - first]
    }

    return u
}

/** Approximate unit tangent at [left end][end] of region */
private fun computeLeftTangent(d: List<Vec2F>, end: Int) =
    (d[end + 1] - d[end]).normalized()

/** Approximate unit tangent at [right end][end] of region */
private fun computeRightTangent(d: List<Vec2F>, end: Int) =
    (d[end - 1] - d[end]).normalized()


private fun computeCenterTangent(d: List<Vec2F>, center: Int): Vec2F {
    val tHatCenter = Vec2F.zero()
    val v1 = d[center - 1] - d[center]
    val v2 = d[center] - d[center + 1]
    tHatCenter.x = (v1.x + v2.x) / 2f
    tHatCenter.y = (v1.y + v2.y) / 2f
    return tHatCenter.normalized()
}

/** Bezier multiplier */
private fun b0(u: Float): Float {
    val tmp = 1f - u
    return tmp * tmp * tmp
}

/** Bezier multiplier */
private fun b1(u: Float): Float {
    val tmp = 1f - u
    return 3f * u * (tmp * tmp)
}

/** Bezier multiplier */
private fun b2(u: Float): Float {
    val tmp = 1f - u
    return 3f * u * u * tmp
}

/** Bezier multiplier */
private fun b3(u: Float): Float =
    u * u * u