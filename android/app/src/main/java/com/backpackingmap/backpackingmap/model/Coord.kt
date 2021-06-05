package com.backpackingmap.backpackingmap.model

import com.mapbox.mapboxsdk.geometry.LatLng
import gov.nasa.worldwind.geom.Angle
import gov.nasa.worldwind.geom.coords.UTMCoord

class Coord private constructor(val lat: Double, val lng: Double, private var _wwUtm: UTMCoord?) {
    constructor(latlng: LatLng) :
            this(latlng.latitude, latlng.longitude)

    constructor(lat: Double, lng: Double) :
            this(lat, lng, null)

    constructor(hemi: Hemisphere, zone: Int, east: Double, north: Double) :
            this(UTMCoord.fromUTM(zone, hemi.toWWId(), east, north))

    private constructor(utm: UTMCoord) :
            this(utm.latitude.degrees, utm.longitude.degrees, utm)

    init {
        if (lat < -90.0) {
            throw IllegalArgumentException("Lat too small: $lat (lng: $lng)")
        }
        if (lat > 90.0) {
            throw IllegalArgumentException("Lat too big: $lat (lng: $lng)")
        }
        if (lng < -180) {
            throw IllegalArgumentException("Lng too small: $lng (lat: $lat)")
        }
        if (lng > 180) {
            throw IllegalArgumentException("Lng too big: $lng (lat: $lat)")
        }
    }

    val zone: Int get() = wwUtm().zone

    val east: Double get() = wwUtm().easting

    val north: Double get() = wwUtm().northing

    val hemi: Hemisphere
        get() {
            val cached = _hemi
            return if (cached != null) {
                cached
            } else {
                val new = Hemisphere.fromWWId(wwUtm().hemisphere)
                _hemi = new
                new
            }
        }

    val gridZone: Char
        get() {
            val cached = _mgrsGridZone
            return if (cached != null) {
                cached
            } else {
                val new = computeMgrsGridZone(wwUtm())
                _mgrsGridZone = new
                new
            }
        }

    fun toMapbox(): LatLng {
        val cached = _mapbox
        return if (cached != null) {
            cached
        } else {
            val new = LatLng(lat, lng)
            _mapbox = new
            new
        }
    }

    fun copyLatLng(lat: Double? = null, lng: Double? = null) =
        Coord(lat ?: this.lat, lng ?: this.lng)

    fun copyUtm(
        hemi: Hemisphere? = null,
        zone: Int? = null,
        east: Double? = null,
        north: Double? = null
    ) =
        Coord(hemi ?: this.hemi, zone ?: this.zone, east ?: this.east, north ?: this.north)

    private var _mapbox: LatLng? = null
    private var _hemi: Hemisphere? = null
    private var _mgrsGridZone: Char? = null

    private fun wwUtm(): UTMCoord {
        val cached = _wwUtm
        if (cached != null) {
            return cached
        }

        val wwLat = Angle.fromDegrees(lat)
        val wwLng = Angle.fromDegrees(lng)
        val new = UTMCoord.fromLatLon(wwLat, wwLng)
        _wwUtm = new
        return new
    }

    companion object {
        val ZERO = Coord(0.0, 0.0)

        private fun computeMgrsGridZone(point: UTMCoord): Char {
            val latitude = point.latitude.radians

            val temp: Double
            val latDeg: Double = latitude * RAD_TO_DEG

            val letter = if (latDeg >= 72 && latDeg < 84.5) {
                LETTER_X
            } else if (latDeg > -80.5 && latDeg < 72) {
                temp = (latitude + 80.0 * DEG_TO_RAD) / (8.0 * DEG_TO_RAD) + 1.0e-12
                latitudeBandConstants[temp.toInt()]
            } else {
                throw IllegalArgumentException("Invalid point")
            }

            return ALPHABET[letter]
        }

        const val MIN_ZONE = 1
        const val MAX_ZONE = 60
    }

    override fun hashCode(): Int {
        return (lat to lng).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this.hashCode() == other.hashCode()
    }

    override fun toString(): String {
        return "Coord(lat = $lat, lng = $lng)"
    }
}

@Suppress("SpellCheckingInspection")
private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val RAD_TO_DEG = 57.29577951308232087 // 180/PI
private const val DEG_TO_RAD = 0.017453292519943295 // PI/180

private const val LETTER_C = 2 /* ARRAY INDEX FOR LETTER C */
private const val LETTER_D = 3 /* ARRAY INDEX FOR LETTER D */
private const val LETTER_E = 4 /* ARRAY INDEX FOR LETTER E */
private const val LETTER_F = 5 /* ARRAY INDEX FOR LETTER E */
private const val LETTER_G = 6 /* ARRAY INDEX FOR LETTER H */
private const val LETTER_H = 7 /* ARRAY INDEX FOR LETTER H */
private const val LETTER_J = 9 /* ARRAY INDEX FOR LETTER J */
private const val LETTER_K = 10 /* ARRAY INDEX FOR LETTER J */
private const val LETTER_L = 11 /* ARRAY INDEX FOR LETTER L */
private const val LETTER_M = 12 /* ARRAY INDEX FOR LETTER M */
private const val LETTER_N = 13 /* ARRAY INDEX FOR LETTER N */
private const val LETTER_P = 15 /* ARRAY INDEX FOR LETTER P */
private const val LETTER_Q = 16 /* ARRAY INDEX FOR LETTER Q */
private const val LETTER_R = 17 /* ARRAY INDEX FOR LETTER R */
private const val LETTER_S = 18 /* ARRAY INDEX FOR LETTER S */
private const val LETTER_T = 19 /* ARRAY INDEX FOR LETTER S */
private const val LETTER_U = 20 /* ARRAY INDEX FOR LETTER U */
private const val LETTER_V = 21 /* ARRAY INDEX FOR LETTER V */
private const val LETTER_W = 22 /* ARRAY INDEX FOR LETTER W */
private const val LETTER_X = 23 /* ARRAY INDEX FOR LETTER X */

private val latitudeBandConstants = arrayOf(
    LETTER_C,
    LETTER_D,
    LETTER_E,
    LETTER_F,
    LETTER_G,
    LETTER_H,
    LETTER_J,
    LETTER_K,
    LETTER_L,
    LETTER_M,
    LETTER_N,
    LETTER_P,
    LETTER_Q,
    LETTER_R,
    LETTER_S,
    LETTER_T,
    LETTER_U,
    LETTER_V,
    LETTER_W,
    LETTER_X,
)