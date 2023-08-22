/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2023 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nadmm.airports.utils

import android.hardware.GeomagneticField
import android.location.Location
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    const val METERS_PER_STATUTE_MILE = 1609.344.toFloat()
    const val METERS_PER_NAUTICAL_MILE = 1852.0.toFloat()
    const val STATUTE_MILES_PER_NAUTICAL_MILES = 1.151.toFloat()
    const val NAUTICAL_MILES_PER_STATUTE_MILES = 0.869.toFloat()

    // Earth's radius at major semi-axis in nautical miles
    private const val WGS84_a = 6378137.0 / METERS_PER_NAUTICAL_MILE

    // Earth's radius at minor semi-axis in nautical miles
    private const val WGS84_b = 6356752.3 / METERS_PER_NAUTICAL_MILE

    // Limits in radians
    private const val MIN_LAT = -Math.PI / 2
    private const val MAX_LAT = Math.PI / 2
    private const val MIN_LON = -Math.PI
    private const val MAX_LON = Math.PI
    private const val TWO_MINUTES = 1000 * 60 * 2
    private fun getEarthRadius(radLat: Double): Double {
        // Earth radius in nautical miles at a given latitude, according to the WGS-84 ellipsoid
        // http://en.wikipedia.org/wiki/Earth_radius
        val an = WGS84_a * WGS84_a * cos(radLat)
        val bn = WGS84_b * WGS84_b * sin(radLat)
        val ad = WGS84_a * cos(radLat)
        val bd = WGS84_b * sin(radLat)
        return sqrt((an * an + bn * bn) / (ad * ad + bd * bd))
    }

    @JvmStatic
    fun getBoundingBoxRadians(location: Location, r: Int): DoubleArray {
        val radLat = Math.toRadians(location.latitude)
        val radLon = Math.toRadians(location.longitude)

        // Calculate the radius of earth at this latitude
        val er = getEarthRadius(radLat)
        // Calculate the angular distance
        val radDist = r / er
        var radLatMin = radLat - radDist
        var radLatMax = radLat + radDist
        var radLonMin: Double
        var radLonMax: Double
        if (radLatMin > MIN_LAT && radLatMax < MAX_LAT) {
            val deltaLon = asin(sin(radDist) / cos(radLat))
            radLonMin = radLon - deltaLon
            if (radLonMin < MIN_LON) radLonMin += 2 * Math.PI
            radLonMax = radLon + deltaLon
            if (radLonMax > MAX_LON) radLonMax -= 2 * Math.PI
        } else {
            // A pole is within the bounding box
            radLatMin = radLatMin.coerceAtLeast(MIN_LAT)
            radLatMax = radLatMax.coerceAtMost(MAX_LAT)
            radLonMin = MIN_LON
            radLonMax = MAX_LON
        }
        return doubleArrayOf(radLatMin, radLatMax, radLonMin, radLonMax)
    }

    @JvmStatic
    fun getBoundingBoxDegrees(location: Location, r: Int): DoubleArray {
        val box = getBoundingBoxRadians(location, r)
        box[0] = Math.toDegrees(box[0])
        box[1] = Math.toDegrees(box[1])
        box[2] = Math.toDegrees(box[2])
        box[3] = Math.toDegrees(box[3])
        return box
    }

    @JvmStatic
    fun getCardinalDirection(bearing: Float): String {
        if (bearing >= 348.76 || bearing <= 11.25) {
            return "N"
        } else if (bearing in 11.26..33.75) {
            return "NNE"
        } else if (bearing in 33.76..56.25) {
            return "NE"
        } else if (bearing in 56.26..78.75) {
            return "ENE"
        } else if (bearing in 78.76..101.25) {
            return "E"
        } else if (bearing in 101.26..123.75) {
            return "ESE"
        } else if (bearing in 123.76..146.25) {
            return "SE"
        } else if (bearing in 146.26..168.75) {
            return "SSE"
        } else if (bearing in 168.76..191.25) {
            return "S"
        } else if (bearing in 191.26..213.75) {
            return "SSW"
        } else if (bearing in 213.76..236.25) {
            return "SW"
        } else if (bearing in 236.26..258.75) {
            return "WSW"
        } else if (bearing in 258.76..281.25) {
            return "W"
        } else if (bearing in 281.26..303.75) {
            return "WNW"
        } else if (bearing in 303.76..326.25) {
            return "NW"
        } else if (bearing in 326.26..348.75) {
            return "NNW"
        }

        // Just to satisfy the compiler
        return "???"
    }

    @JvmStatic
    fun getMagneticDeclination(location: Location): Float {
        val geoField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )
        // West declination is reported in negative values
        return -1 * geoField.declination
    }

    @JvmStatic
    fun applyDeclination(windDir: Double, declination: Double): Double {
        return (windDir + declination + 360) % 360
    }

    fun applyDeclination(heading: Long, declination: Float): Long {
        return ((heading + declination + 360).roundToInt() % 360).toLong()
    }

    fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true
        }
        if (location.latitude == currentBestLocation.latitude
            && location.longitude == currentBestLocation.longitude
        ) {
            // No change in location is not better
            return false
        }

        // Check whether the new location fix is newer or older
        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > TWO_MINUTES
        val isSignificantlyOlder = timeDelta < -TWO_MINUTES
        val isNewer = timeDelta > 0

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false
        }

        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        // Check if the old and new location are from the same provider
        val isFromSameProvider = isSameProvider(
            location.provider,
            currentBestLocation.provider
        )

        // Determine location quality using a combination of timeliness and accuracy
        return if (isMoreAccurate) {
            true
        } else if (isNewer && !isLessAccurate) {
            true
        } else isNewer && !isSignificantlyLessAccurate && isFromSameProvider
    }

    private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
        return if (provider1 == null) {
            provider2 == null
        } else provider1 == provider2
    }
}