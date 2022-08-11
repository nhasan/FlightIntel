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

import android.content.Context
import android.database.Cursor
import com.nadmm.airports.R
import android.widget.TextView
import android.view.View
import androidx.cursoradapter.widget.ResourceCursorAdapter
import com.nadmm.airports.data.DatabaseManager.DOF
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import java.util.Locale
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.GeoUtils

class DofCursorAdapter(context: Context?, c: Cursor?) :
    ResourceCursorAdapter(context, R.layout.dof_list_item, c, 0) {
    internal class ViewHolder {
        var obstacleType: TextView? = null
        var mslHeight: TextView? = null
        var aglHeight: TextView? = null
        var markingType: TextView? = null
        var lightingType: TextView? = null
        var location: TextView? = null
    }

    override fun bindView(view: View, context: Context, c: Cursor) {
        var holder : ViewHolder? = null;
        if (view.tag == null) {
            holder = ViewHolder()
            holder.obstacleType = view.findViewById(R.id.obstacle_type)
            holder.mslHeight = view.findViewById(R.id.height_msl)
            holder.aglHeight = view.findViewById(R.id.height_agl)
            holder.markingType = view.findViewById(R.id.marking_type)
            holder.lightingType = view.findViewById(R.id.lighting_type)
            holder.location = view.findViewById(R.id.location)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }
        var obstacleType = decodeObstacle(c.getString(c.getColumnIndex(DOF.OBSTACLE_TYPE)))
        val count = c.getInt(c.getColumnIndex(DOF.COUNT))
        val mslHeight = c.getInt(c.getColumnIndex(DOF.HEIGHT_MSL))
        val aglHeight = c.getInt(c.getColumnIndex(DOF.HEIGHT_AGL))
        val marking = decodeMarking(c.getString(c.getColumnIndex(DOF.MARKING_TYPE)))
        val lighting = decodeLighting(c.getString(c.getColumnIndex(DOF.LIGHTING_TYPE)))
        val distance = c.getFloat(c.getColumnIndex(LocationColumns.DISTANCE))
        val bearing = c.getFloat(c.getColumnIndex(LocationColumns.BEARING))
        if (count > 1) {
            obstacleType = String.format(Locale.US, "%s (%d count)", obstacleType, count)
        }
        holder.obstacleType!!.text = obstacleType
        holder.mslHeight!!.text = FormatUtils.formatFeetMsl(mslHeight.toFloat())
        holder.aglHeight!!.text = FormatUtils.formatFeetAgl(aglHeight.toFloat())
        holder.markingType!!.text = marking
        holder.lightingType!!.text = lighting
        holder.location!!.text = String.format(
            Locale.US, "%.1f NM %s, heading %.0f\u00B0 M",
            distance, GeoUtils.getCardinalDirection(bearing), bearing
        )
    }

    private fun decodeObstacle(type: String): String {
        return type.replace("TWR", "TOWER")
            .replace("BLDG", "BUILDING")
    }

    private fun decodeMarking(type: String): String {
        return when (type) {
            "P" -> "Orange/White paint marker"
            "W" -> "White paint marker"
            "M" -> "Marked"
            "F" -> "Flag marker"
            "S" -> "Spherical marker"
            "N" -> "Not marked"
            else -> "Unknown marking"
        }
    }

    private fun decodeLighting(type: String): String {
        return when (type) {
            "R" -> "Red lighting"
            "D" -> "Medium intensity White Strobe & Red lighting"
            "H" -> "High intensity White Strobe & Red lighting"
            "M" -> "Medium intensity White Strobe lighting"
            "S" -> "High intensity White Strobe lighting"
            "F" -> "Flood lighting"
            "C" -> "Dual medium catenary lighting"
            "W" -> "Synchronized Red lighting"
            "L" -> "Lighted"
            "N" -> "Not lighted"
            else -> "Unknown lighting"
        }
    }
}