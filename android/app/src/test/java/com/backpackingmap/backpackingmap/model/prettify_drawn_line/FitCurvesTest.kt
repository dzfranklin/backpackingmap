package com.backpackingmap.backpackingmap.model.prettify_drawn_line

import com.backpackingmap.backpackingmap.model.Vec2F
import junit.framework.Assert.assertEquals
import org.junit.Test

class FitCurvesTest {
    /*
          0   1   2   3   4   5   6    7   8   9   10
      0 |   |   |   |   |   |   |   |   |   |   |   |
      1 |   |   |   |   |   |   |   |   |   |   |   |
      2 |   | x |   |   |   |   |   |   |   |   |   |
      3 |   |   |   |   |   |   |   |   |   |   |   |
      4 |   |   |   |   |   |   |   |   |   |   |   |
      5 |   |   |   |   |   |   |   |   |   |   |   |
      6 |   |   |   |   |   |   |   |   |   |   |   |
      7 |   |   |   |   |   |   |   |   |   |   |   |
      8 |   |   |   |   |   |   |   |   |   |   |   |
      9 |   |   |   |   |   |   |   |   |   |   |   |
     10 |   |   |   |   |   |   |   |   |   |   |   |
     11 |   |   |   |   |   |   |   |   |   |   |   |
     12 |   |   |   |   |   |   |   |   |   |   |   |
     13 |   |   |   |   |   |   |   |   |   |   | x |
     14 |   |   |   |   |   |   |   |   |   |   |   |
     15 |   |   |   |   |   |   |   |   |   |   |   |
     16 |   |   |   |   |   |   |   |   |   |   |   |
     17 |   |   |   |   |   |   |   | x |   |   |   |
     18 |   |   |   |   |   |   |   |   | x |   |   |
    */

    // NOTE: These points must not contain duplicates.
    // In this case they came from rdpReduce(RdpReduceTest.samplePoints, 2f)
    private val samplePoints = listOf(
        Vec2F(1.0f, 2.0f),
        Vec2F(7.0f, 17.0f),
        Vec2F(8.0f, 18.0f),
        Vec2F(10.0f, 13.0f)
    )

    // NOTE: Comes from the original I ported from with squared error 4.0,
    // GraphicsGems FitCurves.c (commit 8ffc343ad959c134a36bbbcee46b5d82f676c92d)
    @Suppress("SpellCheckingInspection")
    val expectedFromSampleWithSquaredError4 = listOf(
        CubicBezier(
            Vec2F(1.000000f, 2.000000f),
            Vec2F(3.360883f, 3.967403f),
            Vec2F(6.156091f, 10.458545f),
            Vec2F(8.000000f, 8.000000f),
        ),
        CubicBezier(
            Vec2F(8.000000f, 8.000000f),
            Vec2F(9.077033f, 6.563956f),
            Vec2F(11.269296f, 4.269296f),
            Vec2F(10.000000f, 3.000000f),
        ),
        CubicBezier(
            Vec2F(10.000000f, 3.000000f),
            Vec2F(7.539196f, 0.539196f),
            Vec2F(0.000000f, 0.000000f),
            Vec2F(0.000000f, 0.000000f),
        )
    )

    @Test
    fun computesCorrectForSample() {
        val actual = fitCurves(samplePoints, 2f)
        assertEquals(expectedFromSampleWithSquaredError4, actual)
    }
}