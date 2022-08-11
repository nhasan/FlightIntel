/*
 * FlightIntel for Pilots
 *
 * Copyright 2018-2022 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.provider.BaseColumns
import com.nadmm.airports.data.DatabaseManager.DOF
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.utils.DbUtils
import com.nadmm.airports.utils.GeoUtils
import java.util.*

class NearbyDofCursor(db: SQLiteDatabase?, location: Location, radius: Int) : MatrixCursor(
    sColumns
) {
    private inner class DOFData : Comparable<DOFData> {
        var oasCode: String? = null
        var verificationStatus: String? = null
        var obstacleType: String? = null
        var count = 0
        var heightAgl = 0
        var heightMsl = 0
        var lightingType: String? = null
        var markingType: String? = null
        var bearing = 0f
        var distance = 0f

        fun setFromCursor(c: Cursor, location: Location, declination: Float) {
            oasCode = c.getString(c.getColumnIndex(DOF.OAS_CODE))
            verificationStatus = c.getString(c.getColumnIndex(DOF.VERIFICATION_STATUS))
            obstacleType = c.getString(c.getColumnIndex(DOF.OBSTACLE_TYPE))
            count = c.getInt(c.getColumnIndex(DOF.COUNT))
            heightAgl = c.getInt(c.getColumnIndex(DOF.HEIGHT_AGL))
            heightMsl = c.getInt(c.getColumnIndex(DOF.HEIGHT_MSL))
            lightingType = c.getString(c.getColumnIndex(DOF.LIGHTING_TYPE))
            markingType = c.getString(c.getColumnIndex(DOF.MARKING_TYPE))
            val results = FloatArray(2)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                c.getDouble(c.getColumnIndex(DOF.LATITUDE_DEGREES)),
                c.getDouble(c.getColumnIndex(DOF.LONGITUDE_DEGREES)),
                results
            )
            distance = results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE
            bearing = (results[1] + declination + 360) % 360
        }

        override fun compareTo(other: DOFData): Int {
            if (heightMsl > other.heightMsl) {
                return -1
            } else if (heightMsl < other.heightMsl) {
                return 1
            }
            return 0
        }
    }

    companion object {
        private val sColumns = arrayOf(
            BaseColumns._ID,
            DOF.OAS_CODE,
            DOF.VERIFICATION_STATUS,
            DOF.OBSTACLE_TYPE,
            DOF.COUNT,
            DOF.HEIGHT_AGL,
            DOF.HEIGHT_MSL,
            DOF.LIGHTING_TYPE,
            DOF.MARKING_TYPE,
            LocationColumns.BEARING,
            LocationColumns.DISTANCE
        )
    }

    init {
        val c = DbUtils.getBoundingBoxCursor(
            db, DOF.TABLE_NAME,
            DOF.LATITUDE_DEGREES, DOF.LONGITUDE_DEGREES, location, radius
        )
        if (c.moveToFirst()) {
            val declination = GeoUtils.getMagneticDeclination(location)
            val dofList = arrayOfNulls<DOFData>(c.count)
            do {
                val obst = DOFData()
                obst.setFromCursor(c, location, declination)
                dofList[c.position] = obst
            } while (c.moveToNext())

            // Sort the list based on distance from current location
            Arrays.sort(dofList)
            for (dof in dofList) {
                if (dof!!.distance <= radius) {
                    if (!location.hasAltitude() || location.altitude - 100 <= dof.heightMsl) {
                        val row = newRow()
                        row.add(position)
                            .add(dof.oasCode)
                            .add(dof.verificationStatus)
                            .add(dof.obstacleType)
                            .add(dof.count)
                            .add(dof.heightAgl)
                            .add(dof.heightMsl)
                            .add(dof.lightingType)
                            .add(dof.markingType)
                            .add(dof.bearing)
                            .add(dof.distance)
                    }
                }
            }
        }
        c.close()
    }
}