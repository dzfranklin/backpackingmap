package com.backpackingmap.backpackingmap.map

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.closeTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.locationtech.proj4j.CRSFactory

class CoordinateTest {
    private lateinit var crsFactory: CRSFactory
    private lateinit var subject: Coordinate

    @Before
    fun setUp() {
        crsFactory = CRSFactory()
        // WGS84 datum
        val crs = crsFactory.createFromName("EPSG:4326")
        subject = Coordinate(crs, -2.804904, 56.340259)
    }

    @Test
    fun convertTo() {
        val newCrs = crsFactory.createFromName("EPSG:27700")
        val converted = subject.convertTo(newCrs)

        assertThat(converted.crs, `is`(newCrs))
        assertThat(converted.x, closeTo(350339.62, 0.01))
        assertThat(converted.y, closeTo(716724.23, 0.01))
    }

    @Test
    fun movedBy() {
        // Reference computed using <https://www.movable-type.co.uk/scripts/latlong.html>
        val dest = Coordinate(subject.crs, -2.803904, 56.340259)
        val moved = subject.movedBy(111.2f, 0.0f)

        assertThat(moved.crs, `is`(subject.crs))
        assertThat(moved.x, closeTo(dest.x, 0.0005))
        assertThat(moved.y, closeTo(dest.y, 0.0005))
    }
}