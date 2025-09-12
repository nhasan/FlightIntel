/*
 * FlightIntel for Pilots
 *
 * Copyright 2018-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import androidx.core.util.Pair
import com.nadmm.airports.utils.GeoUtils.getBoundingBoxRadians

object DbUtils {
    fun getBoundingBoxSelection(
        latField: String, lonField: String,
        location: Location, radius: Int
    ): Pair<String, Array<String>> {
        val box = getBoundingBoxRadians(location, radius)
        val radLatMin = box[0]
        val radLatMax = box[1]
        val radLonMin = box[2]
        val radLonMax = box[3]

        // Check if 180th Meridian lies within the bounding Box
        val isCrossingMeridian180 = (radLonMin > radLonMax)

        val selection = ("("
                + latField + ">=? AND " + latField + "<=?" + ") AND (" + lonField + ">=? "
                + (if (isCrossingMeridian180) "OR " else "AND ") + lonField + "<=?)")
        val selectionArgs = arrayOf(
            Math.toDegrees(radLatMin).toString(),
            Math.toDegrees(radLatMax).toString(),
            Math.toDegrees(radLonMin).toString(),
            Math.toDegrees(radLonMax).toString()
        )

        return Pair<String, Array<String>>(selection, selectionArgs)
    }

    fun getBoundingBoxCursor(
        db: SQLiteDatabase, tableName: String,
        latField: String, lonField: String,
        location: Location, radius: Int
    ): Cursor {
        return getBoundingBoxCursor(db, tableName, arrayOf("*"), latField, lonField, location, radius)
    }

    fun getBoundingBoxCursor(
        db: SQLiteDatabase, tableName: String, columns: Array<String>,
        latField: String, lonField: String,
        location: Location, radius: Int
    ): Cursor {
        val selection = getBoundingBoxSelection(latField, lonField, location, radius)
        val builder = SQLiteQueryBuilder()
        builder.tables = tableName

        val c = builder.query(
            db, columns, selection.first, selection.second,
            null, null, null, null
        )

        return c
    }
}
