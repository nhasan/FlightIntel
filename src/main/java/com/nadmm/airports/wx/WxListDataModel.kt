/*
 * FlightIntel for Pilots
 *
 * Copyright 2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.database.Cursor
import android.location.Location
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.GeoUtils

data class WxListDataModel(
    val stationId: String,
    val stationName: String,
    val stationInfo: String,
    val stationInfo2: String,
    val stationFreq: String,
    val stationPhone: String,
    val declination: Float
) {
    companion object {
        fun fromCursor(cursor: Cursor): WxListDataModel {
            val stationId = cursor.getString(cursor.getColumnIndexOrThrow(Wxs.STATION_ID))
            val stationName = cursor.getString(cursor.getColumnIndexOrThrow(Wxs.STATION_NAME)) ?: ""

            val stationInfo = buildString {
                val city = cursor.getString(cursor.getColumnIndexOrThrow(Airports.ASSOC_CITY))
                if (city != null && city.isNotEmpty()) {
                    append(city)
                }
                val state = cursor.getString(cursor.getColumnIndexOrThrow(Airports.ASSOC_STATE))
                if (state != null && state.isNotEmpty()) {
                    if (isNotEmpty()) {
                        append(", ")
                    }
                    append(state)
                }
                if (isEmpty()) {
                    append("Location unknown")
                }
            }

            val stationInfo2 = buildString {
                var type = cursor.getString(cursor.getColumnIndexOrThrow(Awos1.WX_SENSOR_TYPE))
                if (type == null || type.isEmpty()) {
                    type = "ASOS/AWOS"
                }
                append(type)
                val distanceColIndex = cursor.getColumnIndex(LocationColumns.DISTANCE)
                val bearingColIndex = cursor.getColumnIndex(LocationColumns.BEARING)
                if ( distanceColIndex >= 0 && bearingColIndex >= 0) {
                    val distance = cursor.getFloat(distanceColIndex)
                    val bearing = cursor.getFloat(bearingColIndex)
                    append(", ")
                    append(FormatUtils.formatNauticalMiles(distance))
                    append(" ")
                    append(GeoUtils.getCardinalDirection(bearing))
                } else {
                    append(", ")
                    val elevation = cursor.getInt(cursor.getColumnIndexOrThrow(Wxs.STATION_ELEVATOIN_METER))
                    append(FormatUtils.formatFeetMsl(DataUtils.metersToFeet(elevation).toFloat()))
                }
            }

            val stationFreq = buildString {
                var freq = cursor.getString(cursor.getColumnIndexOrThrow(Awos1.STATION_FREQUENCY))
                if (freq == null || freq.isEmpty()) {
                    freq = cursor.getString(cursor.getColumnIndexOrThrow(Awos1.SECOND_STATION_FREQUENCY))
                }
                if (freq != null && freq.isNotEmpty()) {
                    try {
                        append(FormatUtils.formatFreq(java.lang.Float.valueOf(freq)))
                    } catch (_: NumberFormatException) {
                        append(freq)
                    }
                }
            }

            val stationPhone = cursor.getString(cursor.getColumnIndexOrThrow(Awos1.STATION_PHONE_NUMBER)) ?: ""

            val lat = cursor.getDouble(
                cursor.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES)
            )
            val lon = cursor.getDouble(
                cursor.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES)
            )
            val location = Location("")
            location.latitude = lat
            location.longitude = lon
            val declination = GeoUtils.getMagneticDeclination(location)

            return WxListDataModel(stationId, stationName, stationInfo, stationInfo2, stationFreq, stationPhone, declination)
        }
    }
}