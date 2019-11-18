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

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.TextView
import androidx.cursoradapter.widget.ResourceCursorAdapter
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.GeoUtils
import java.util.*

class AirportsCursorAdapter(context: Context, c: Cursor)
    : ResourceCursorAdapter(context, R.layout.airport_list_item, c, 0) {

    internal class ViewHolder {
        var name: TextView? = null
        var id: TextView? = null
        var location: TextView? = null
        var distance: TextView? = null
        var other: TextView? = null

        companion object Factory {
            fun create(view: View): ViewHolder {
                val holder = ViewHolder()
                holder.name = view.findViewById(R.id.facility_name)
                holder.id = view.findViewById(R.id.facility_id)
                holder.location = view.findViewById(R.id.location)
                holder.distance = view.findViewById(R.id.distance)
                holder.other = view.findViewById(R.id.other_info)
                return holder
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun bindView(view: View, context: Context, c: Cursor) {
        var holder: ViewHolder? = view.tag as? ViewHolder
        if (holder == null) {
            holder = ViewHolder.create(view)
            view.tag = holder
        }

        val name = c.getString(c.getColumnIndex(Airports.FACILITY_NAME))
        val siteNumber = c.getString(c.getColumnIndex(Airports.SITE_NUMBER))
        val type = DataUtils.decodeLandingFaclityType(siteNumber)
        holder.name?.text = "$name $type"
        var id: String? = c.getString(c.getColumnIndex(Airports.ICAO_CODE))
        if (id == null || id.trim().isEmpty()) {
            id = c.getString(c.getColumnIndex(Airports.FAA_CODE))
        }
        holder.id?.text = id
        val city = c.getString(c.getColumnIndex(Airports.ASSOC_CITY))
        val state = c.getString(c.getColumnIndex(Airports.ASSOC_STATE))
        val use = c.getString(c.getColumnIndex(Airports.FACILITY_USE))
        holder.location?.text = "$city, $state, ${DataUtils.decodeFacilityUse(use)}"

        val other = arrayListOf<String>()
        val fuel = c.getString(c.getColumnIndex(Airports.FUEL_TYPES))
        val elev = c.getFloat(c.getColumnIndex(Airports.ELEVATION_MSL))
        val ctaf = c.getString(c.getColumnIndex(Airports.CTAF_FREQ))
        val unicom = c.getString(c.getColumnIndex(Airports.UNICOM_FREQS))
        val status = c.getString(c.getColumnIndex(Airports.STATUS_CODE))
        if (status == "O") {
            other.add(FormatUtils.formatFeetMsl(elev))
            if (ctaf != null && ctaf.isNotEmpty()) {
                other.add(ctaf)
            } else if (unicom != null && unicom.isNotEmpty()) {
                other.add(unicom)
            }
            if (fuel != null && fuel.isNotEmpty()) {
                other.add(DataUtils.decodeFuelTypes(fuel))
            }
        } else {
            other.add(DataUtils.decodeStatus(status))
        }
        holder.other?.text = other.joinToString()

        val distance = c.getFloat(c.getColumnIndex(LocationColumns.DISTANCE))
        val bearing = c.getFloat(c.getColumnIndex(LocationColumns.BEARING))
        if (distance >= 0 && bearing >= 0) {
            // Check if we have distance information
            holder.distance?.text = String.format(Locale.US,
                    "%.1f NM %s, initial course %.0f\u00B0 M",
                    distance, GeoUtils.getCardinalDirection(bearing), bearing)
        } else {
            holder.distance?.visibility = View.GONE
        }
    }

}
