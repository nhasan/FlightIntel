/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import android.location.Location
import android.text.format.DateUtils
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

class SolarCalculator(private val mLocation: Location, private val mTimeZone: TimeZone) {

    fun getSunriseTime(zenith: Double, date: Calendar): Calendar? {
        date.setTimeZone(mTimeZone)
        val longitudeHour = getSunriseLongitudeHour(date)
        val meanAnomaly = getMeanAnomaly(longitudeHour)
        val sunTrueLongitude = getSunTrueLongitude(meanAnomaly)
        val cosineSunLocalHour = getCosineSunLocalHour(sunTrueLongitude, zenith)

        if (cosineSunLocalHour < -1.0 || cosineSunLocalHour > 1.0) {
            // Sun does not rise
            return null
        }

        val sunLocalHour = getSunLocalHourForSunrise(cosineSunLocalHour)
        val localMeanTime = getLocalMeanTime(sunTrueLongitude, longitudeHour, sunLocalHour)
        val localTime = getLocalTime(localMeanTime, date)

        return getTimeAsCalendar(localTime, date)
    }

    fun getSunsetTime(zenith: Double, date: Calendar): Calendar? {
        date.setTimeZone(mTimeZone)
        val longitudeHour = getSunsetLongitudeHour(date)
        val meanAnomaly = getMeanAnomaly(longitudeHour)
        val sunTrueLongitude = getSunTrueLongitude(meanAnomaly)
        val cosineSunLocalHour = getCosineSunLocalHour(sunTrueLongitude, zenith)

        if (cosineSunLocalHour < -1.0 || cosineSunLocalHour > 1.0) {
            // Sun does not set
            return null
        }

        val sunLocalHour = getSunLocalHourForSunset(cosineSunLocalHour)
        val localMeanTime = getLocalMeanTime(sunTrueLongitude, longitudeHour, sunLocalHour)
        val localTime = getLocalTime(localMeanTime, date)

        return getTimeAsCalendar(localTime, date)
    }

    private val baseLongitudeHour: Double
        get() = mLocation.getLongitude() / 15.0

    private fun getSunriseLongitudeHour(date: Calendar): Double {
        return date.get(Calendar.DAY_OF_YEAR) + ((6 - this.baseLongitudeHour) / 24)
    }

    private fun getSunsetLongitudeHour(date: Calendar): Double {
        return date.get(Calendar.DAY_OF_YEAR) + ((18 - this.baseLongitudeHour) / 24)
    }

    private fun getMeanAnomaly(longitudeHour: Double): Double {
        return (longitudeHour * 0.9856) - 3.289
    }

    private fun getSunTrueLongitude(meanAnomaly: Double): Double {
        var sunTrueLongitude = (meanAnomaly
                + (1.916 * sin(Math.toRadians(meanAnomaly)))
                + (0.020 * sin(Math.toRadians(meanAnomaly * 2)))
                + 282.634)
        if (sunTrueLongitude < 0) {
            sunTrueLongitude += 360.0
        } else if (sunTrueLongitude > 360) {
            sunTrueLongitude -= 360.0
        }
        return sunTrueLongitude
    }

    private fun getRightAscention(sunTrueLongitude: Double): Double {
        val tanL = tan(Math.toRadians(sunTrueLongitude))
        var rightAscention = Math.toDegrees(atan(tanL * 0.91764))
        if (rightAscention < 0) {
            rightAscention += 360.0
        } else if (rightAscention > 360) {
            rightAscention -= 360.0
        }
        val longitudeQuadrant = floor(sunTrueLongitude / 90) * 90
        val rightAscensionQuadrant = floor(rightAscention / 90) * 90
        return (rightAscention + (longitudeQuadrant - rightAscensionQuadrant)) / 15
    }

    private fun getSineOfSunDeclination(sunTrueLong: Double): Double {
        return sin(Math.toRadians(sunTrueLong)) * 0.39782
    }

    private fun getCosineOfSunDeclination(sineSunDeclination: Double): Double {
        return cos(asin(sineSunDeclination))
    }

    private fun getCosineSunLocalHour(sunTrueLong: Double, zenith: Double): Double {
        val sineSunDeclination = getSineOfSunDeclination(sunTrueLong)
        val cosineSunDeclination = getCosineOfSunDeclination(sineSunDeclination)
        val sineLatitude = sin(Math.toRadians(mLocation.getLatitude()))
        val cosineLatitude = cos(Math.toRadians(mLocation.getLatitude()))
        val dividend = cos(Math.toRadians(zenith)) - (sineSunDeclination * sineLatitude)
        val divisor = cosineSunDeclination * cosineLatitude
        return dividend / divisor
    }

    private fun getSunLocalHourForSunrise(cosineSunLocalHour: Double): Double {
        return (360 - Math.toDegrees(acos(cosineSunLocalHour))) / 15
    }

    private fun getSunLocalHourForSunset(cosineSunLocalHour: Double): Double {
        return Math.toDegrees(acos(cosineSunLocalHour)) / 15
    }

    private fun getLocalMeanTime(
        sunTrueLongitude: Double, longitudeHour: Double,
        sunLocalHour: Double
    ): Double {
        val rightAscention = getRightAscention(sunTrueLongitude)
        var localMeanTime = sunLocalHour + rightAscention - (longitudeHour * 0.06571) - 6.622
        if (localMeanTime < 0) {
            localMeanTime += 24.0
        } else if (localMeanTime > 24) {
            localMeanTime -= 24.0
        }
        return localMeanTime
    }

    private fun adjustForDST(localMeanTime: Double, date: Calendar): Double {
        var localTime = localMeanTime
        if (mTimeZone.inDaylightTime(date.getTime())) {
            localTime += (mTimeZone.getDSTSavings() / DateUtils.HOUR_IN_MILLIS).toDouble()
        }
        if (localTime > 24) {
            localTime -= 24.0
        }
        return localTime
    }

    private fun getUtcTime(localMeanTime: Double): Double {
        return localMeanTime - baseLongitudeHour
    }

    private fun getLocalTime(localMeanTime: Double, date: Calendar): Double {
        val utcTime = getUtcTime(localMeanTime)
        val utcOffset = (date.get(Calendar.ZONE_OFFSET) / DateUtils.HOUR_IN_MILLIS).toDouble()
        val localTime = utcTime + utcOffset
        return adjustForDST(localTime, date)
    }

    private fun getTimeAsCalendar(time: Double, date: Calendar): Calendar {
        var hours = floor(time).toInt()
        var minutes = ((time - hours) * 60).toInt()
        if (minutes == 60) {
            minutes = 0
            ++hours
        }

        val result = date.clone() as Calendar
        result.set(Calendar.HOUR_OF_DAY, hours)
        result.set(Calendar.MINUTE, minutes)
        result.set(Calendar.SECOND, 0)

        return result
    }

    companion object {
        const val CIVIL: Double = 96.0
        const val OFFICIAL: Double = 90.8333
    }
}
