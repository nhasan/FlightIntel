/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.utils.DataUtils.calculateRadial
import com.nadmm.airports.utils.DataUtils.getMorseCode
import com.nadmm.airports.utils.DataUtils.isDirectionalNavaid
import com.nadmm.airports.utils.DbUtils
import com.nadmm.airports.utils.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class NearbyNavaidsFragment : FragmentBase() {
    private val mNavColumns = arrayOf(
        Nav1.NAVAID_ID,
        Nav1.NAVAID_TYPE,
        Nav1.NAVAID_NAME,
        Nav1.NAVAID_FREQUENCY,
        Nav1.TACAN_CHANNEL,
        LocationColumns.RADIAL,
        LocationColumns.DISTANCE
    )
    private var mRadius = 0

    private class NavaidData : Comparable<NavaidData> {
        var navaidId: String? = null
        var navaidType: String? = null
        var navaidName: String? = null
        var navaidFreq: String? = null
        var tacanChannel: String? = null
        var radial = 0
        var distance = 0f

        fun setFromCursor(c: Cursor, location: Location) {
            // Calculate the distance and bearing to this navaid from this airport
            navaidId = c.getString(c.getColumnIndexOrThrow(Nav1.NAVAID_ID))
            navaidType = c.getString(c.getColumnIndexOrThrow(Nav1.NAVAID_TYPE))
            navaidName = c.getString(c.getColumnIndexOrThrow(Nav1.NAVAID_NAME))
            navaidFreq = c.getString(c.getColumnIndexOrThrow(Nav1.NAVAID_FREQUENCY))
            tacanChannel = c.getString(c.getColumnIndexOrThrow(Nav1.TACAN_CHANNEL))
            var variation = c.getInt(c.getColumnIndexOrThrow(Nav1.MAGNETIC_VARIATION_DEGREES))
            val dir = c.getString(c.getColumnIndexOrThrow(Nav1.MAGNETIC_VARIATION_DIRECTION))
            if (dir == "E") {
                variation *= -1
            }
            val results = FloatArray(2)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                c.getDouble(c.getColumnIndexOrThrow(Nav1.REF_LATTITUDE_DEGREES)),
                c.getDouble(c.getColumnIndexOrThrow(Nav1.REF_LONGITUDE_DEGREES)),
                results)
            distance = results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE
            radial = calculateRadial(results[1], variation)
        }

        override fun compareTo(other: NavaidData): Int {
            if (distance > other.distance) {
                return 1
            } else if (distance < other.distance) {
                return -1
            }
            return 0
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.airport_navaids_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRadius = activityBase.prefNearbyRadius
        setActionBarTitle("Nearby Navaids", "")
        setActionBarSubtitle(String.format(Locale.US, "Within %d NM radius", mRadius))
        arguments?.getString(Airports.SITE_NUMBER)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                showDetails(result)
            }
        }
    }

    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0]
        val vor = result[1]
        val ndb = result[2]
        if (vor != null || ndb != null) {
            showAirportTitle(apt!!)
            showNavaidDetails(result)
        } else {
            setContentMsg(String.format(Locale.US, "No navaids found within %d NM radius",
                mRadius))
        }
        setFragmentContentShown(true)
    }

    private fun showNavaidDetails(result: Array<Cursor?>) {
        val vor = result[1]
        if (vor != null && vor.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.detail_navaids_vor_layout)
            do {
                val navaidId = vor.getString(vor.getColumnIndexOrThrow(Nav1.NAVAID_ID))
                var freq = vor.getString(vor.getColumnIndexOrThrow(Nav1.NAVAID_FREQUENCY))
                if (freq.isEmpty()) {
                    freq = vor.getString(vor.getColumnIndexOrThrow(Nav1.TACAN_CHANNEL))
                }
                val name = vor.getString(vor.getColumnIndexOrThrow(Nav1.NAVAID_NAME))
                val type = vor.getString(vor.getColumnIndexOrThrow(Nav1.NAVAID_TYPE))
                val radial = vor.getInt(vor.getColumnIndexOrThrow("RADIAL"))
                val distance = vor.getFloat(vor.getColumnIndexOrThrow("DISTANCE"))
                addDirectionalNavaidRow(layout, navaidId, name, type, freq, radial,
                    distance)
            } while (vor.moveToNext())
        } else {
            val layout = findViewById<LinearLayout>(R.id.detail_navaids_vor_layout)
            layout!!.visibility = View.GONE
            val tv = findViewById<TextView>(R.id.detail_navaids_vor_label)
            tv!!.visibility = View.GONE
        }
        val ndb = result[2]
        if (ndb != null && ndb.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.detail_navaids_ndb_layout)
            do {
                val navaidId = ndb.getString(ndb.getColumnIndexOrThrow(Nav1.NAVAID_ID))
                val freq = ndb.getString(ndb.getColumnIndexOrThrow(Nav1.NAVAID_FREQUENCY))
                val name = ndb.getString(ndb.getColumnIndexOrThrow(Nav1.NAVAID_NAME))
                val type = ndb.getString(ndb.getColumnIndexOrThrow(Nav1.NAVAID_TYPE))
                val heading = ndb.getInt(ndb.getColumnIndexOrThrow("RADIAL"))
                val distance = ndb.getFloat(ndb.getColumnIndexOrThrow("DISTANCE"))
                addNonDirectionalNavaidRow(layout, navaidId, name, type, freq, heading,
                    distance)
            } while (ndb.moveToNext())
        } else {
            val layout = findViewById<LinearLayout>(R.id.detail_navaids_ndb_layout)
            layout!!.visibility = View.GONE
            val tv = findViewById<TextView>(R.id.detail_navaids_ndb_label)
            tv!!.visibility = View.GONE
        }
    }

    private fun addDirectionalNavaidRow(
        table: LinearLayout?, navaidId: String,
        name: String, type: String, freq: String, radial: Int, distance: Float
    ) {
        val label1 = navaidId + "      " + getMorseCode(navaidId)
        val label2 = "$name $type"
        val value2 = String.format(Locale.US, "r%03d/%.1fNM", radial, distance)
        val args = Bundle()
        args.putString(Nav1.NAVAID_ID, navaidId)
        args.putString(Nav1.NAVAID_TYPE, type)
        addClickableRow(table!!,
            label1,
            freq,
            label2,
            value2,
            NavaidDetailsFragment::class.java,
            args)
    }

    private fun addNonDirectionalNavaidRow(
        table: LinearLayout?, navaidId: String,
        name: String, type: String, freq: String, heading: Int, distance: Float
    ) {
        val label1 = navaidId + "      " + getMorseCode(navaidId)
        val label2 = "$name $type"
        val value2 = String.format(Locale.US, "%03d\u00B0M/%.1fNM", heading, distance)
        val args = Bundle()
        args.putString(Nav1.NAVAID_ID, navaidId)
        args.putString(Nav1.NAVAID_TYPE, type)
        addClickableRow(table!!,
            label1,
            freq,
            label2,
            value2,
            NavaidDetailsFragment::class.java,
            args)
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val db = getDatabase(DB_FADDS)
        val cursors = arrayOfNulls<Cursor>(3)
        val apt = getAirportDetails(siteNumber)
        cursors[0] = apt
        val lat = apt!!.getDouble(apt.getColumnIndexOrThrow(Airports.REF_LATTITUDE_DEGREES))
        val lon = apt.getDouble(apt.getColumnIndexOrThrow(Airports.REF_LONGITUDE_DEGREES))
        val location = Location("")
        location.latitude = lat
        location.longitude = lon
        val c = DbUtils.getBoundingBoxCursor(db, Nav1.TABLE_NAME,
            Nav1.REF_LATTITUDE_DEGREES, Nav1.REF_LONGITUDE_DEGREES,
            location, mRadius)
        if (c.moveToFirst()) {
            val navaids = arrayOfNulls<NavaidData>(c.count)
            do {
                val navaid = NavaidData()
                navaid.setFromCursor(c, location)
                navaids[c.position] = navaid
            } while (c.moveToNext())

            // Sort the navaids list by distance from current location
            Arrays.sort(navaids)
            val vor = MatrixCursor(mNavColumns)
            val ndb = MatrixCursor(mNavColumns)
            for (navaid in navaids) {
                if (navaid!!.distance <= mRadius) {
                    if (isDirectionalNavaid(navaid.navaidType!!)) {
                        val row = vor.newRow()
                        row.add(navaid.navaidId)
                            .add(navaid.navaidType)
                            .add(navaid.navaidName)
                            .add(navaid.navaidFreq)
                            .add(navaid.tacanChannel)
                            .add(navaid.radial)
                            .add(navaid.distance)
                    } else {
                        val row = ndb.newRow()
                        row.add(navaid.navaidId)
                            .add(navaid.navaidType)
                            .add(navaid.navaidName)
                            .add(navaid.navaidFreq)
                            .add(navaid.tacanChannel)
                            .add(navaid.radial)
                            .add(navaid.distance)
                    }
                }
            }
            cursors[1] = vor
            cursors[2] = ndb
        }
        c.close()
        return cursors
    }
}