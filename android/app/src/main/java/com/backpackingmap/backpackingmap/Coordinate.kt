package com.backpackingmap.backpackingmap

import com.backpackingmap.backpackingmap.map.MapState
import com.backpackingmap.backpackingmap.map.NaiveCoordinate
import com.backpackingmap.backpackingmap.map.ZoomLevel
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.locationtech.proj4j.units.Units
import org.locationtech.proj4j.util.ProjectionMath
import kotlin.math.cos
import kotlin.math.round

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
        val newEastingUnnormalized = easting + metersEast / (111_111.0 * cos(northing))
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

    fun movedBy(zoom: ZoomLevel, deltaX: Float, deltaY: Float): Coordinate {
        val metersNorth = -1 * deltaY * zoom.metersPerPixel

        // and then invert deltaX again (so not at all) because east is to the left
        val metersEast = deltaX * zoom.metersPerPixel

        return movedBy(metersEast, metersNorth)
    }

    /** May return coordinates outside the visible space covered by the state */
    fun toScreen(state: MapState): NaiveCoordinate {
        if (crs.projection.units != Units.DEGREES) {
            return this.convertTo(wgs84).toScreen(state)
        }
        val center = state.center.convertTo(crs)
        val axisOrder = crs.projection.axisOrder
        val pixelsPerMeter = 1 / state.zoom.metersPerPixel

        val thisNormalized = ProjCoordinate(x, y)
        axisOrder.toENU(thisNormalized)
        val thisEasting = thisNormalized.x
        val thisNorthing = thisNormalized.y

        val centerNormalized = ProjCoordinate(center.x, center.y)
        axisOrder.toENU(centerNormalized)
        val centerEasting = centerNormalized.x
        val centerNorthing = centerNormalized.y

        val avgNorthing = (centerNorthing + thisNorthing) / 2.0

        // pixels relative to center
        val pixelsEast =
            (thisEasting - centerEasting) * (111_111.0 * cos(centerNorthing)) * pixelsPerMeter
        val pixelsNorth = (thisNorthing - centerNorthing) * 111_111.0 * pixelsPerMeter

        val centerX = round(state.size.width / 2.0)
        val centerY = round(state.size.height / 2.0)

        val x = centerX + pixelsEast
        val y = centerY - pixelsNorth

        return NaiveCoordinate(x, y)
    }
}

fun NaiveCoordinate.asCrs(crsName: String): Coordinate {
    val crs = crsFactory.createFromName(crsName)
    return Coordinate(crs, this.x, this.y)
}

fun NaiveCoordinate.asWgs84() = Coordinate(wgs84, x, y)
