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
package com.nadmm.airports.afd

import android.database.Cursor
import android.os.Bundle
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.GeoUtils

data class AirportListDataModel(
    val siteNumber: String,
    val icaoCode: String?,
    val faaCode: String,
    val facilityId: String,
    val facilityName: String,
    val location: String,
    val otherInfo: String,
    val position: String?
) {
    fun makeBundle(): Bundle {
        return Bundle().apply {
            putString(Airports.SITE_NUMBER, siteNumber)
            putString(Airports.FAA_CODE, faaCode)
            putString(Airports.ICAO_CODE, icaoCode)
        }
    }

    companion object {
        fun fromCursor(cursor: Cursor): AirportListDataModel {
            val siteNumber = cursor.getString(cursor.getColumnIndexOrThrow(Airports.SITE_NUMBER))
            val icaoCode = cursor.getString(cursor.getColumnIndexOrThrow(Airports.ICAO_CODE))
            val faaCode = cursor.getString(cursor.getColumnIndexOrThrow(Airports.FAA_CODE))
            var facilityId = icaoCode
            if (facilityId == null || facilityId.trim().isEmpty()) {
                facilityId = faaCode
            }
            val facilityName = cursor.getString(cursor.getColumnIndexOrThrow(Airports.FACILITY_NAME))
            val city = cursor.getString(cursor.getColumnIndexOrThrow(Airports.ASSOC_CITY))
            val state = cursor.getString(cursor.getColumnIndexOrThrow(Airports.ASSOC_STATE))
            val use = cursor.getString(cursor.getColumnIndexOrThrow(Airports.FACILITY_USE))
            val location = "$city, $state, ${DataUtils.decodeFacilityUse(use)}"

            val other = arrayListOf<String>()
            val fuel = cursor.getString(cursor.getColumnIndexOrThrow(Airports.FUEL_TYPES))
            val elev = cursor.getFloat(cursor.getColumnIndexOrThrow(Airports.ELEVATION_MSL))
            val ctaf = cursor.getString(cursor.getColumnIndexOrThrow(Airports.CTAF_FREQ))
            val unicom = cursor.getString(cursor.getColumnIndexOrThrow(Airports.UNICOM_FREQS))
            val status = cursor.getString(cursor.getColumnIndexOrThrow(Airports.STATUS_CODE))
            if (status == "O") {
                other.add(FormatUtils.formatFeetMsl(elev))
                if (ctaf.isNotEmpty()) {
                    other.add(ctaf)
                } else if (unicom.isNotEmpty()) {
                    other.add(unicom)
                }
                if (fuel.isNotEmpty()) {
                    other.add(DataUtils.decodeFuelTypes(fuel))
                }
            } else {
                other.add(DataUtils.decodeStatus(status))
            }
            val otherInfo = other.joinToString()

            var position: String? = null
            val distanceColIndex = cursor.getColumnIndex(LocationColumns.DISTANCE)
            val bearingColIndex = cursor.getColumnIndex(LocationColumns.BEARING)
            if ( distanceColIndex >= 0 && bearingColIndex >= 0 ) {
                val distance = cursor.getFloat(distanceColIndex)
                val bearing = cursor.getFloat(bearingColIndex)
                // Check if we have distance information
                position = "%.1f NM %s, initial course %.0f\u00B0 M"
                    .format(distance, GeoUtils.getCardinalDirection(bearing), bearing)
            }

            return AirportListDataModel(
                siteNumber,
                icaoCode,
                faaCode,
                facilityId,
                facilityName,
                location,
                otherInfo,
                position)
        }
    }
}