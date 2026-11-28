/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.dof

import android.database.sqlite.SQLiteDatabase
import com.nadmm.airports.data.DatabaseManager.DOF
import org.osmdroid.util.BoundingBox

/** An obstacle as loaded for an area: what choosing and drawing it need. */
class ObstacleRow(
    val latitude: Double,
    val longitude: Double,
    val aglHeight: Int,
    val mslHeight: Int,
    val type: String,
    val count: Int,
    val lightingCode: String?,
    val markingCode: String?,
) {
    val isLighted = DofListDataModel.isLighted(lightingCode)
    val typeName = DofListDataModel.typeName(type)
}

/**
 * The obstacles in [box], tallest first, held so the map can show the tallest
 * in its view as it moves without searching the database each time. Even with
 * the database's index on position, loading a wide area takes a good part of
 * a second; choosing from those held takes a few milliseconds.
 */
class ObstacleArea(val box: BoundingBox, val rows: List<ObstacleRow>) {

    /** True when [view] lies within this area. */
    fun covers(view: BoundingBox): Boolean = covers(box, view)

    companion object {
        /** True when [view] lies within [box]. */
        fun covers(box: BoundingBox, view: BoundingBox): Boolean =
            view.latNorth <= box.latNorth && view.latSouth >= box.latSouth &&
                view.lonEast <= box.lonEast && view.lonWest >= box.lonWest

        fun load(db: SQLiteDatabase, box: BoundingBox): ObstacleArea {
            // The same type and code texts repeat over thousands of rows
            val shared = HashMap<String, String>()
            fun share(text: String?) = text?.let { shared.getOrPut(it) { it } }

            val rows = ArrayList<ObstacleRow>()
            db.query(DOF.TABLE_NAME,
                arrayOf(DOF.LATITUDE_DEGREES, DOF.LONGITUDE_DEGREES, DOF.HEIGHT_AGL,
                    DOF.HEIGHT_MSL, DOF.OBSTACLE_TYPE, DOF.COUNT, DOF.LIGHTING_TYPE,
                    DOF.MARKING_TYPE),
                "${DOF.LATITUDE_DEGREES} BETWEEN ? AND ? AND ${DOF.LONGITUDE_DEGREES} BETWEEN ? AND ?",
                arrayOf(box.latSouth.toString(), box.latNorth.toString(),
                    box.lonWest.toString(), box.lonEast.toString()),
                null, null, null
            ).use { c ->
                rows.ensureCapacity(c.count)
                while (c.moveToNext()) {
                    rows.add(ObstacleRow(
                        c.getDouble(0), c.getDouble(1), c.getInt(2), c.getInt(3),
                        share(c.getString(4)).orEmpty(), c.getInt(5),
                        share(c.getString(6)), share(c.getString(7))))
                }
            }
            rows.sortByDescending { it.aglHeight }
            return ObstacleArea(box, rows)
        }
    }
}
