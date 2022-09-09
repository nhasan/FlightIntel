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

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.MatrixCursor
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.utils.DbUtils
import com.nadmm.airports.utils.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@SuppressLint("Range")
class NearbyFssFragment : FragmentBase() {
    private var mRadius = 0

    private class ComData(c: Cursor, declination: Float, location: Location) : Comparable<ComData> {
        val mColumnValues: Array<String?>
        override fun compareTo(other: ComData): Int {
            // Last element in the value array is the distance
            val indexOfDistance = mColumnValues.size - 1
            val distance1 = mColumnValues[indexOfDistance]!!.toDouble()
            val distance2 = other.mColumnValues[indexOfDistance]!!.toDouble()
            if (distance1 > distance2) {
                return 1
            } else if (distance1 < distance2) {
                return -1
            }
            return 0
        }

        init {
            mColumnValues = arrayOfNulls(c.columnCount + 2)
            var i = 0
            while (i < c.columnCount) {
                mColumnValues[i] = c.getString(i)
                ++i
            }

            // Now calculate the distance to this wx station
            val results = FloatArray(2)
            Location.distanceBetween(
                location.latitude, location.longitude,
                c.getDouble(c.getColumnIndex(Com.COMM_OUTLET_LATITUDE_DEGREES)),
                c.getDouble(c.getColumnIndex(Com.COMM_OUTLET_LONGITUDE_DEGREES)),
                results
            )
            // Bearing
            mColumnValues[i] = ((results[1] + declination + 360) % 360).toString()
            ++i
            // Distance
            mColumnValues[i] = (results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fss_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRadius = activityBase.prefNearbyRadius
        setActionBarTitle("Nearby FSS", "")
        setActionBarSubtitle("Within $mRadius NM radius")

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
        showAirportTitle(apt!!)
        showFssDetails(result)
        setFragmentContentShown(true)
    }

    @SuppressLint("SetTextI18n")
    private fun showFssDetails(result: Array<Cursor?>) {
        val com = result[1]
        val detailLayout = findViewById<LinearLayout>(R.id.fss_detail_layout)
        if (com!!.moveToFirst()) {
            do {
                val outletId = com.getString(com.getColumnIndex(Com.COMM_OUTLET_ID))
                val outletType = com.getString(com.getColumnIndex(Com.COMM_OUTLET_TYPE))
                val outletCall = com.getString(com.getColumnIndex(Com.COMM_OUTLET_CALL))
                val navId = com.getString(com.getColumnIndex(Com.ASSOC_NAVAID_ID))
                val navName = com.getString(com.getColumnIndex(Nav1.NAVAID_NAME))
                val navType = com.getString(com.getColumnIndex(Nav1.NAVAID_TYPE))
                val navFreq = com.getString(com.getColumnIndex(Nav1.NAVAID_FREQUENCY))
                val freqs = com.getString(com.getColumnIndex(Com.COMM_OUTLET_FREQS))
                val fssName = com.getString(com.getColumnIndex(Com.FSS_NAME))
                val bearing = com.getFloat(com.getColumnIndex(BEARING))
                val distance = com.getFloat(com.getColumnIndex(DISTANCE))
                val item = inflate<RelativeLayout>(R.layout.grouped_detail_item)
                var tv = item.findViewById<TextView>(R.id.group_name)
                if (navId.isNotEmpty()) {
                    tv.text = "$navId - $navName $navType"
                } else {
                    tv.text = "$outletId - $outletCall outlet"
                }
                tv = item.findViewById(R.id.group_extra)
                if (distance < 1.0) {
                    tv.text = "On-site"
                } else {
                    tv.text = String.format(
                        Locale.US, "%.0f NM %s", distance,
                        GeoUtils.getCardinalDirection(bearing)
                    )
                }
                val layout = item.findViewById<LinearLayout>(R.id.group_details)
                addRow(layout, "Call", "$fssName Radio")
                if (navId.isNotEmpty()) {
                    addRow(layout, navId, navFreq + "T")
                }
                var i = 0
                while (i < freqs.length) {
                    val end = (i + 9).coerceAtMost(freqs.length)
                    val freq = freqs.substring(i, end).trim { it <= ' ' }
                    addRow(layout, outletType, freq)
                    i = end
                }
                detailLayout!!.addView(
                    item, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            } while (com.moveToNext())
        } else {
            setContentMsg(
                String.format(
                    Locale.US,
                    "No FSS outlets found within %dNM radius.", mRadius
                )
            )
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val db = getDatabase(DB_FADDS)
        val result = arrayOfNulls<Cursor>(2)
        val apt = getAirportDetails(siteNumber)
        result[0] = apt
        val lat = apt!!.getDouble(apt.getColumnIndex(Airports.REF_LATTITUDE_DEGREES))
        val lon = apt.getDouble(apt.getColumnIndex(Airports.REF_LONGITUDE_DEGREES))
        val location = Location("")
        location.latitude = lat
        location.longitude = lon
        val tableName = (Com.TABLE_NAME + " c LEFT OUTER JOIN " + Nav1.TABLE_NAME + " n"
                + " ON c." + Com.ASSOC_NAVAID_ID + " = n." + Nav1.NAVAID_ID
                + " AND n." + Nav1.NAVAID_TYPE + " <> 'VOT'")
        val columns = arrayOf(
            "c.*", "n." + Nav1.NAVAID_NAME,
            "n." + Nav1.NAVAID_TYPE, "n." + Nav1.NAVAID_FREQUENCY
        )
        val c = DbUtils.getBoundingBoxCursor(
            db, tableName, columns,
            Com.COMM_OUTLET_LATITUDE_DEGREES, Com.COMM_OUTLET_LONGITUDE_DEGREES,
            location, mRadius
        )
        val columnNames = arrayOfNulls<String>(c.columnCount + 2)
        var i = 0
        for (col in c.columnNames) {
            columnNames[i++] = col
        }
        columnNames[i++] = BEARING
        columnNames[i] = DISTANCE
        val matrix = MatrixCursor(columnNames)
        if (c.moveToFirst()) {
            // Now find the magnetic declination at this location
            val declination = GeoUtils.getMagneticDeclination(location)
            val comDataList = arrayOfNulls<ComData>(c.count)
            var row = 0
            do {
                val com = ComData(c, declination, location)
                comDataList[row++] = com
            } while (c.moveToNext())

            // Sort the FSS Com list by distance
            Arrays.sort(comDataList)

            // Build a cursor out of the sorted FSS station list
            for (com in comDataList) {
                val distance = com!!.mColumnValues[matrix.getColumnIndex(DISTANCE)]?.toFloatOrNull()
                if (distance != null && distance <= mRadius) {
                    matrix.addRow(com.mColumnValues)
                }
            }
        }
        c.close()
        result[1] = matrix
        return result
    }

    companion object {
        // Extra column names for the cursor
        private const val DISTANCE = "DISTANCE"
        private const val BEARING = "BEARING"
    }
}