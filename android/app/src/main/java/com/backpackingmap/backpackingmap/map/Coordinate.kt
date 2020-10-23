package com.backpackingmap.backpackingmap.map

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.locationtech.proj4j.units.Units
import org.locationtech.proj4j.util.ProjectionMath
import kotlin.math.cos

private val transformFactory = CoordinateTransformFactory()
val crsFactory = CRSFactory()
private val wgs84: CoordinateReferenceSystem = crsFactory.createFromName("EPSG:4326")

data class Coordinate(
    val crs: CoordinateReferenceSystem,
    val x: Double,
    val y: Double,
) {
    fun convertTo(newCrs: CoordinateReferenceSystem): Coordinate {
        if (newCrs == crs) {
            return this
        }

        val transform = transformFactory.createTransform(crs, newCrs)
        val source = ProjCoordinate(x, y)
        val target = ProjCoordinate()
        transform.transform(source, target)

        return Coordinate(newCrs, target.x, target.y)
    }

    fun movedBy(metersEast: Float, metersNorth: Float): Coordinate {
        if (crs.projection.units != Units.DEGREES) {
            return this.convertTo(wgs84).movedBy(metersEast, metersNorth)
        }

        val normalizedCoords = ProjCoordinate(x, y)
        // ENU means East, North, Up
        crs.projection.axisOrder.toENU(normalizedCoords)
        val easting = normalizedCoords.x
        val northing = normalizedCoords.y

        // Approximation, see <https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters/2964#2964>
        val newEastingUnnormalized = easting + metersEast / (111_111 * cos(northing))
        val newNorthingUnnormalized = northing + metersNorth / 111_111.0

        val newEasting = ProjectionMath.radToDeg(
            ProjectionMath.normalizeLongitude(
                ProjectionMath.degToRad(newEastingUnnormalized)))

        val newNorthing = ProjectionMath.radToDeg(
            ProjectionMath.normalizeLatitude(
                ProjectionMath.degToRad(newNorthingUnnormalized)))

        val denormalizedCoords = ProjCoordinate(newEasting, newNorthing)
        crs.projection.axisOrder.fromENU(denormalizedCoords)

        return Coordinate(crs, denormalizedCoords.x, denormalizedCoords.y)
    }
}

fun NaiveCoordinate.toCoordinate(crsName: String): Coordinate {
    val crs = crsFactory.createFromName(crsName)
    return Coordinate(crs, this.x, this.y)
}
