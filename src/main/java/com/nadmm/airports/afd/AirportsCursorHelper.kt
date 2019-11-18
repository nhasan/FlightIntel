/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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
import android.database.sqlite.SQLiteQueryBuilder
import android.provider.BaseColumns
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.States
import java.util.*

object AirportsCursorHelper {
    private val mQueryColumns = arrayOf(
            BaseColumns._ID,
            Airports.SITE_NUMBER,
            Airports.ICAO_CODE,
            Airports.FAA_CODE,
            Airports.FACILITY_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Airports.FUEL_TYPES,
            Airports.UNICOM_FREQS,
            Airports.CTAF_FREQ,
            Airports.ELEVATION_MSL,
            Airports.STATUS_CODE,
            Airports.FACILITY_USE,
            Airports.REF_LATTITUDE_DEGREES,
            Airports.REF_LONGITUDE_DEGREES,
            States.STATE_NAME
    )

    private fun buildProjectionMap(): HashMap<String, String> {
        val map = HashMap<String, String>()
        for (col in mQueryColumns) {
            if (col != States.STATE_NAME) {
                map[col] = col
            }
        }
        map[States.STATE_NAME] = "IFNULL(${States.STATE_NAME}, ${Airports.ASSOC_COUNTY})" +
                " AS ${States.STATE_NAME}"
        return map
    }

    @JvmStatic
    fun query(db: SQLiteDatabase?, selection: String?, selectionArgs: Array<String>?)
            : Cursor {
        return query(db, selection, selectionArgs, null, null, null, null)
    }

    @JvmStatic
    fun query(db: SQLiteDatabase?, selection: String?, selectionArgs: Array<String>?,
              groupBy: String?, having: String?, sortOrder: String?, limit: String?)
            : Cursor {
        if (db == null) {
            return MatrixCursor(mQueryColumns)
        }
        val builder = SQLiteQueryBuilder()
        builder.tables = "${Airports.TABLE_NAME} a LEFT OUTER JOIN ${States.TABLE_NAME} s" +
                " ON a.${Airports.ASSOC_STATE} = s.${States.STATE_CODE}"
        builder.projectionMap = buildProjectionMap()
        return builder.query(db, mQueryColumns, selection, selectionArgs,
                groupBy, having, sortOrder, limit)
    }
}