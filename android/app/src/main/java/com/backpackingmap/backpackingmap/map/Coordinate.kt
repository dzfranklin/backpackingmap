package com.backpackingmap.backpackingmap.map

import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

val transformFactory = CoordinateTransformFactory()

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

    fun movedBy(metersX: Double, metersY: Double): Coordinate {
        // NOTE: Does not wrap x & y
        val unitsPerMeter = 1.0 / crs.projection.fromMetres
        val newX = x + (metersX * unitsPerMeter)
        val newY = y + (metersY * unitsPerMeter)
        return Coordinate(crs, newX, newY)
    }
}