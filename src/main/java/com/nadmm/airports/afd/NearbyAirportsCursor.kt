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

package com.nadmm.airports.afd

import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.provider.BaseColumns
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.utils.DbUtils
import com.nadmm.airports.utils.GeoUtils

class NearbyAirportsCursor(db: SQLiteDatabase, location: Location, radius: Int,
                           extraSelection: String?) : MatrixCursor(sColumns) {

    init {

        val declination = GeoUtils.getMagneticDeclination(location)

        val selection = DbUtils.getBoundingBoxSelection(
                Airports.REF_LATTITUDE_DEGREES, Airports.REF_LONGITUDE_DEGREES, location, radius)
        var select = selection.first
        val selectionArgs = selection.second

        if (extraSelection != null && extraSelection.isNotEmpty()) {
            select = "($select) $extraSelection"
        }

        val c = AirportsCursorHelper.query(db, select, selectionArgs)
        if (c.moveToFirst()) {
            val airports = arrayListOf<AirportData>()
            do {
                val airport = AirportData()
                airport.setFromCursor(c, location, declination)
                airports.add(airport)
            } while (c.moveToNext())

            // Sort the airport list by distance from current location
            val sortedAirports = airports.sortedWith(compareBy { d -> d.distance })

            // Build a cursor out of the sorted airport list
            for ( airport: AirportData in sortedAirports) {
                if (airport.distance.toInt() in 0..radius) {
                    val row = newRow()
                    row.add(airport.id)
                            .add(airport.siteNumber)
                            .add(airport.icaoCode)
                            .add(airport.faaCode)
                            .add(airport.facilityNAme)
                            .add(airport.assocCity)
                            .add(airport.assocState)
                            .add(airport.fuelTypes)
                            .add(airport.ctafFreq)
                            .add(airport.unicomFreq)
                            .add(airport.elevationMSL)
                            .add(airport.statusCode)
                            .add(airport.facilityUse)
                            .add(airport.stateNAme)
                            .add(airport.distance)
                            .add(airport.bearing)
                }
            }
        }
        c.close()
    }

    // This data class allows us to sort the airport list based in distance
    private inner class AirportData {

        var id: Long = -1
        var siteNumber: String? = null
        var icaoCode: String? = null
        var faaCode: String? = null
        var facilityNAme: String? = null
        var assocCity: String? = null
        var assocState: String? = null
        var fuelTypes: String? = null
        var unicomFreq: String? = null
        var ctafFreq: String? = null
        var elevationMSL: String? = null
        var statusCode: String? = null
        var facilityUse: String? = null
        var stateNAme: String? = null
        var lattitude: Double = 0.toDouble()
        var longitude: Double = 0.toDouble()
        var distance: Float = 0.toFloat()
        var bearing: Float = 0.toFloat()

        fun setFromCursor(c: Cursor, location: Location, declination: Float) {
            id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID))
            siteNumber = c.getString(c.getColumnIndexOrThrow(Airports.SITE_NUMBER))
            facilityNAme = c.getString(c.getColumnIndexOrThrow(Airports.FACILITY_NAME))
            icaoCode = c.getString(c.getColumnIndexOrThrow(Airports.ICAO_CODE))
            faaCode = c.getString(c.getColumnIndexOrThrow(Airports.FAA_CODE))
            assocCity = c.getString(c.getColumnIndexOrThrow(Airports.ASSOC_CITY))
            assocState = c.getString(c.getColumnIndexOrThrow(Airports.ASSOC_STATE))
            fuelTypes = c.getString(c.getColumnIndexOrThrow(Airports.FUEL_TYPES))
            elevationMSL = c.getString(c.getColumnIndexOrThrow(Airports.ELEVATION_MSL))
            unicomFreq = c.getString(c.getColumnIndexOrThrow(Airports.UNICOM_FREQS))
            ctafFreq = c.getString(c.getColumnIndexOrThrow(Airports.CTAF_FREQ))
            statusCode = c.getString(c.getColumnIndexOrThrow(Airports.STATUS_CODE))
            facilityUse = c.getString(c.getColumnIndexOrThrow(Airports.FACILITY_USE))
            stateNAme = c.getString(c.getColumnIndexOrThrow(States.STATE_NAME))
            lattitude = c.getDouble(c.getColumnIndexOrThrow(Airports.REF_LATTITUDE_DEGREES))
            longitude = c.getDouble(c.getColumnIndexOrThrow(Airports.REF_LONGITUDE_DEGREES))

            // Now calculate the distance to this airport
            val results = FloatArray(2)
            Location.distanceBetween(location.latitude, location.longitude,
                    lattitude, longitude, results)
            distance = results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE
            bearing = (results[1] + declination + 360f) % 360
        }

    }

    companion object {

        private val sColumns = arrayOf(
                BaseColumns._ID,
                Airports.SITE_NUMBER,
                Airports.ICAO_CODE,
                Airports.FAA_CODE,
                Airports.FACILITY_NAME,
                Airports.ASSOC_CITY,
                Airports.ASSOC_STATE,
                Airports.FUEL_TYPES,
                Airports.CTAF_FREQ,
                Airports.UNICOM_FREQS,
                Airports.ELEVATION_MSL,
                Airports.STATUS_CODE,
                Airports.FACILITY_USE,
                States.STATE_NAME,
                LocationColumns.DISTANCE,
                LocationColumns.BEARING
        )
    }

}
