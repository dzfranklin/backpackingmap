package com.backpackingmap.backpackingmap.model

import android.location.Location
import java.time.Instant

/**
 * See <https://developer.android.com/reference/android/location/Location.html> for more
 * detailed documentation of fields
 */
data class TrackMoment(
    val lat: Double,
    val lng: Double,
    /** Estimated horizontal accuracy of this location, radial, in meters (if available) */
    val accuracy: Float?,
    /** Altitude in meters above the WGS 84 reference ellipsoid (if available) */
    val altitude: Double?,
    /** estimated vertical accuracy of this location, in meters (if available). */
    val verticalAccuracy: Float?,
    /** Bearing, in degrees */
    val bearing: Float?,
    /** Estimated bearing accuracy of this location, in degrees */
    val bearingAccuracy: Float?,
    /** Speed in meters/second over ground (if available) */
    val speed: Float?,
    /** Estimated speed accuracy of this location, in meters per second. */
    val speedAccuracy: Float?,
    val provider: String,
    val timestamp: Instant,
    val elapsed: Long,
) {
    companion object {
        fun fromPlatform(loc: Location) =
            TrackMoment(
                lat = loc.latitude,
                lng = loc.longitude,
                accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                verticalAccuracy = if (loc.hasVerticalAccuracy()) loc.verticalAccuracyMeters else null,
                bearing = if (loc.hasBearing()) loc.bearing else null,
                bearingAccuracy = if (loc.hasBearingAccuracy()) loc.bearingAccuracyDegrees else null,
                timestamp = Instant.ofEpochMilli(loc.time),
                elapsed = loc.elapsedRealtimeNanos,
                provider = loc.provider,
                speed = if (loc.hasSpeed()) loc.speed else null,
                speedAccuracy = if (loc.hasSpeedAccuracy()) loc.speedAccuracyMetersPerSecond else null
            )
    }
}
