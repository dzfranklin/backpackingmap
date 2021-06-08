package com.backpackingmap.backpackingmap.model.prettify_drawn_line

import com.backpackingmap.backpackingmap.model.Vec2F
import org.junit.Assert.assertEquals
import org.junit.Test

class RdpReduceTest {
    /*
          0   1   2   3   4   5   6    7   8   9   10
      0 |   |   |   |   |   |   |   |   |   |   |   |
      1 |   |   |   |   |   |   |   |   |   |   |   |
      2 |   | x | x | x |   |   |   |   |   |   |   |
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
     15 |   |   |   |   |   | x |   |   |   |   |   |
     16 |   |   |   |   |   |   | x |   |   |   |   |
     17 |   |   |   |   |   |   |   | x |   |   |   |
     18 |   |   |   |   |   |   |   |   | x |   |   |
    */

    private val samplePoints = listOf(
        Vec2F(1f, 2f),
        Vec2F(2f, 2f),
        Vec2F(3f, 2f),
        Vec2F(5f, 15f),
        Vec2F(6f, 16f),
        Vec2F(7f, 17f),
        Vec2F(8f, 18f),
        Vec2F(10f, 13f),
    )

    // NOTE: Comes from the original I ported from with error 4.0,
    //   com.jwetherell.algorithms.mathematics.RamerDouglasPeucker.java
    private val expectedForSampleWithEpsilon2 = listOf(
        Vec2F(1.0f,  2.0f),
        Vec2F(7.0f, 17.0f),
        Vec2F(8.0f, 18.0f),
        Vec2F(10.0f, 13.0f)
    )

    @Test
    fun computesCorrectForSample() {
        val actual = rdpReduce(samplePoints, 2f)
        assertEquals(expectedForSampleWithEpsilon2, actual)
    }
}