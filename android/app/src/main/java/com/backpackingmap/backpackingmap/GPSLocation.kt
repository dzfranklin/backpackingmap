package com.backpackingmap.backpackingmap

import android.location.Location
import com.backpackingmap.backpackingmap.map.NaiveCoordinate
import kotlinx.datetime.Instant

sealed class GPSLocation {
    object Unknown : GPSLocation()

    sealed class Error : GPSLocation() {
        object LocationUnavailable : GPSLocation()
        object PermissionNotGranted : GPSLocation()
        data class PlayServicesUnavailable(val cause: Throwable) : GPSLocation()
    }

    /**
     * @property coordinateAccuracy Per the docs, the estimated horizontal accuracy of this
     * location, radial, in meters. We define horizontal accuracy as the radius of 68% confidence.
     * In other words, if you draw a circle centered at this location's latitude and longitude, and
     * with a radius equal to the accuracy, then there is a 68% probability that the true location
     * is inside the circle.
     *
     * @property altitudeAccuracy Per the docs, the estimated vertical accuracy of this location, in
     * meters. We define vertical accuracy at 68% confidence. Specifically, as 1-side of the 2-sided
     * range above and below the estimated altitude reported by getAltitude(), within which there is
     * a 68% probability of finding the true altitude. In the case where the underlying distribution
     * is assumed Gaussian normal, this would be considered 1 standard deviation. For example, if
     * getAltitude() returns 150, and getVerticalAccuracyMeters() returns 20 then there is a 68%
     * probability of the true altitude being between 130 and 170 meters.
     *
     * @property timestamp Not monotonic
     */
    data class Known(
        val coordinate: Coordinate,
        val coordinateAccuracy: Float?,
        val altitude: Double?,
        val altitudeAccuracy: Float?,
        val timestamp: Instant,
    ) : GPSLocation()

    companion object {
        fun fromPlatform(loc: Location?): GPSLocation {
            if (loc == null) {
                return Unknown
            }

            /* NOTE: Documentation for Location says
            (See <https://developer.android.com/reference/android/location/Location.html>)

            All locations returned by getLocations() are guaranteed to have a valid latitude, longitude,
            and UTC timestamp. On API level 17 or later they are also guaranteed to have elapsed
            real-time since boot. All other parameters are optional.
            */

            val horizontalAccuracy: Float? = if (loc.accuracy == 0f) {
                // Returns 0.0 to signify null per docs
                null
            } else {
                loc.accuracy
            }

            val altitude: Double? = loc.altitude
            val altitudeAccuracy: Float? =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (loc.verticalAccuracyMeters == 0f) {
                        // Returns 0.0 to signify null per docs
                        null
                    } else {
                        loc.verticalAccuracyMeters
                    }
                } else {
                    null
                }
            val lat: Double = loc.latitude
            val lng: Double = loc.longitude
            val time: Long = loc.time

            val timestamp = Instant.fromEpochMilliseconds(time)
            val coordinate = NaiveCoordinate(lng, lat).asWgs84()

            return Known(
                coordinate,
                horizontalAccuracy,
                altitude,
                altitudeAccuracy,
                timestamp
            )
        }
    }
}