/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.provider.BaseColumns
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.utils.DbUtils
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.GeoUtils.applyDeclination
import com.nadmm.airports.utils.GeoUtils.getMagneticDeclination

class NearbyWxCursor(db: SQLiteDatabase?, location: Location, radius: Int) : MatrixCursor(sColumns) {
    init {
        val declination = getMagneticDeclination(location).toDouble()

        val selection = DbUtils.getBoundingBoxSelection(
            "x." + Wxs.STATION_LATITUDE_DEGREES, "x." + Wxs.STATION_LONGITUDE_DEGREES, location, radius
        )

        val c = WxCursorHelper.query(db, selection.first, selection.second, null, null, null, null)

        if (c.moveToFirst()) {
            val awosList = listOf<AwosData>().toMutableList()
            do {
                val awos = AwosData(c, location, declination)
                awosList.add(awos)
            } while (c.moveToNext())

            // Sort the airport list by distance from current location
            awosList.sort()

            for (awos in awosList) {
                if (awos.stationStatus == "N" || awos.DISTANCE > radius) {
                    continue
                }
                val row = newRow()
                row.add(position)
                    .add(awos.icaoCode)
                    .add(awos.sensorIdent)
                    .add(awos.sensorType)
                    .add(awos.stationFrequency)
                    .add(awos.secondaryStationFrequency)
                    .add(awos.stationPhoneNumber)
                    .add(awos.stationName)
                    .add(awos.associatedCity)
                    .add(awos.associatedState)
                    .add(awos.stationElevation)
                    .add(awos.stationLatitude)
                    .add(awos.stationLongitude)
                    .add(awos.DISTANCE)
                    .add(awos.BEARING)
            }
        }
        c.close()
    }

    private inner class AwosData(c: Cursor, location: Location, declination: Double) : Comparable<AwosData?> {
        var icaoCode: String
        var stationName: String
        var stationStatus: String
        var sensorIdent: String
        var sensorType: String
        var stationFrequency: String
        var secondaryStationFrequency: String
        var stationPhoneNumber: String
        var associatedCity: String
        var associatedState: String
        var stationElevation: Int
        var stationLatitude: Double
        var stationLongitude: Double
        var DISTANCE: Double
        var BEARING: Double

        init {
            icaoCode = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID)) ?: ""
            stationName = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_NAME)) ?: ""
            stationStatus = c.getString(c.getColumnIndexOrThrow(Awos1.COMMISSIONING_STATUS)) ?: "N"
            sensorIdent = c.getString(c.getColumnIndexOrThrow(Awos1.WX_SENSOR_IDENT)) ?: ""
            sensorType = c.getString(c.getColumnIndexOrThrow(Awos1.WX_SENSOR_TYPE)) ?: ""
            stationFrequency = c.getString(c.getColumnIndexOrThrow(Awos1.STATION_FREQUENCY)) ?: ""
            secondaryStationFrequency = c.getString(c.getColumnIndexOrThrow(Awos1.SECOND_STATION_FREQUENCY)) ?: ""
            stationPhoneNumber = c.getString(c.getColumnIndexOrThrow(Awos1.STATION_PHONE_NUMBER)) ?: ""
            associatedCity = c.getString(c.getColumnIndexOrThrow(Airports.ASSOC_CITY)) ?: ""
            associatedState = c.getString(c.getColumnIndexOrThrow(Airports.ASSOC_STATE)) ?: ""
            stationElevation = c.getInt(c.getColumnIndexOrThrow(Wxs.STATION_ELEVATOIN_METER))
            stationLatitude = c.getDouble(c.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
            stationLongitude = c.getDouble(c.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))

            if (icaoCode.isEmpty()) {
                icaoCode = "K$sensorIdent"
            }

            if (sensorType.isEmpty()) {
                sensorType = "ASOS/AWOS"
            }

            // Now calculate the distance to this wx station
            val results = FloatArray(2)
            Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                stationLatitude, stationLongitude, results
            )
            DISTANCE = (results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE).toDouble()
            BEARING = applyDeclination(results[1].toDouble(), declination)
        }

        override fun compareTo(other: AwosData?): Int {
            if (other == null) {
                return 1 // This instance is greater than null
            }
            if (this.DISTANCE > other.DISTANCE) {
                return 1
            } else if (this.DISTANCE < other.DISTANCE) {
                return -1
            }
            return 0
        }
    }

    companion object {
        // Build a cursor out of the sorted wx station list
        private val sColumns = arrayOf(
            BaseColumns._ID,
            Wxs.STATION_ID,
            Awos1.WX_SENSOR_IDENT,
            Awos1.WX_SENSOR_TYPE,
            Awos1.STATION_FREQUENCY,
            Awos1.SECOND_STATION_FREQUENCY,
            Awos1.STATION_PHONE_NUMBER,
            Wxs.STATION_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Wxs.STATION_ELEVATOIN_METER,
            Wxs.STATION_LATITUDE_DEGREES,
            Wxs.STATION_LONGITUDE_DEGREES,
            LocationColumns.DISTANCE,
            LocationColumns.BEARING
        )
    }
}
