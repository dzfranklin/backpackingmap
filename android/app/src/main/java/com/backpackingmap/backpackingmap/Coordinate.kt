package com.backpackingmap.backpackingmap

import com.backpackingmap.backpackingmap.map.MapState
import com.backpackingmap.backpackingmap.map.ZoomLevel
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.locationtech.proj4j.units.Units

private val transformFactory = CoordinateTransformFactory()
val crsFactory = CRSFactory()
private val wgs84: CoordinateReferenceSystem = crsFactory.createFromName("EPSG:4326")

data class Coordinate(
    val crs: CoordinateReferenceSystem,
    val x: Double,
    val y: Double,
) {
    fun convertTo(newCrsName: String) =
        convertTo(crsFactory.createFromName(newCrsName))

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

    fun convertToWgs84() = convertTo(wgs84)

    fun movedBy(east: Meter, north: Meter): Coordinate {
        if (crs.projection.units != Units.METRES) {
            TODO("Support units other than meters")
        }
        return Coordinate(crs, x + east.toDouble(), north.toDouble() + y)
    }

    fun movedBy(zoom: ZoomLevel, deltaX: Pixel, deltaY: Pixel): Coordinate {
        if (crs.projection.units != Units.METRES) {
            TODO("Support units other than meters")
        }

        // We invert because scrolling moves you in the opposite direction to the one your
        // finger literally moves in
        val metersNorth = -1.0 * (deltaY * zoom.level)

        // and then invert deltaX again (so not at all) because east is to the left
        val metersEast = deltaX * zoom.level

        return movedBy(metersEast, metersNorth)
    }

    /** May return coordinates outside the visible space covered by the state */
    fun toScreenLocation(state: MapState): ScreenLocation {
        if (crs != state.baseCrs) {
            return convertTo(state.baseCrs).toScreenLocation(state)
        }

        if (crs.projection.units != Units.METRES) {
            TODO("Support units other than meters")
        }

        val centerX = state.size.width / 2
        val centerY = state.size.height / 2

        val pixelsPerMeter = state.zoom.level.inverse()

        val pixelsEast = Meter(x - state.center.x) * pixelsPerMeter
        val pixelsNorth = Meter(y - state.center.y) * pixelsPerMeter
        val x = centerX + pixelsEast
        val y = centerY - pixelsNorth

        return ScreenLocation(x, y)
    }
}

fun NaiveCoordinate.asCrs(crsName: String): Coordinate {
    val crs = crsFactory.createFromName(crsName)
    return Coordinate(crs, this.x, this.y)
}

fun NaiveCoordinate.asCrs(crs: CoordinateReferenceSystem) =
    Coordinate(crs, this.x, this.y)

fun NaiveCoordinate.asWgs84() = Coordinate(wgs84, x, y)
