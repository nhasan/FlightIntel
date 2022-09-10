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
import android.database.sqlite.SQLiteQueryBuilder
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
import com.nadmm.airports.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("Range")
class IlsFragment : FragmentBase() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ils_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("ILS details", "")
        arguments?.let {
            val siteNumber = it.getString(Ils1.SITE_NUMBER)
            val runwayId = it.getString(Ils1.RUNWAY_ID)
            val ilsType = it.getString(Ils1.ILS_TYPE)
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(siteNumber!!, runwayId!!, ilsType!!)
                }
                showDetails(result)
            }
        }
    }

    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0]
        showAirportTitle(apt!!)
        val ils1 = result[1]
        if (ils1!!.moveToFirst()) {
            showIlsDetails(result)
            showLocalizerDetails(result)
            showGlideslopeDetails(result)
            showInnerMarkerDetails(result)
            showMiddleMarkerDetails(result)
            showOuterMarkerDetails(result)
            showIlsRemarks(result)
        } else {
            setContentMsg("ILS details not found")
        }
        setFragmentContentShown(true)
    }

    @SuppressLint("SetTextI18n")
    private fun showIlsDetails(result: Array<Cursor?>) {
        val ils1 = result[1]
        val tv = findViewById<TextView>(R.id.rwy_ils_label)
        val rwyId = ils1!!.getString(ils1.getColumnIndex(Ils1.RUNWAY_ID))
        tv!!.text = "Runway $rwyId"
        val layout = findViewById<LinearLayout>(R.id.rwy_ils_details)
        val ilsType = ils1.getString(ils1.getColumnIndex(Ils1.ILS_TYPE))
        addRow(layout!!, "Type", ilsType)
        val locId = ils1.getString(ils1.getColumnIndex(Ils1.ILS_ID))
        addRow(layout, "Id", locId)
        val category = ils1.getString(ils1.getColumnIndex(Ils1.ILS_CATEGORY))
        if (category.isNotEmpty()) {
            addRow(layout, "Category", category)
        }
        val bearing = ils1.getString(ils1.getColumnIndex(Ils1.ILS_MAGNETIC_BEARING))
        addRow(layout, "Magnetic bearing", bearing + "\u00B0")
    }

    private fun showLocalizerDetails(result: Array<Cursor?>) {
        val ils2 = result[2]
        if (ils2!!.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.rwy_loc_details)
            val locFreq = ils2.getString(ils2.getColumnIndex(Ils2.LOCALIZER_FREQUENCY))
            addRow(layout!!, "Frequency", locFreq)
            val locWidth = ils2.getFloat(ils2.getColumnIndex(Ils2.LOCALIZER_COURSE_WIDTH))
            addRow(layout, "Course width", FormatUtils.formatDegrees(locWidth))
            val back = ils2.getString(ils2.getColumnIndex(Ils2.LOCALIZER_BACK_COURSE_STATUS))
            if (back.isNotEmpty()) {
                addRow(layout, "Back course", back)
            }
            val status = ils2.getString(ils2.getColumnIndex(Ils2.OPERATIONAL_STATUS))
            val date = ils2.getString(ils2.getColumnIndex(Ils2.OPERATIONAL_EFFECTIVE_DATE))
            addRow(layout, "Status", status, date)
        } else {
            val tv = findViewById<TextView>(R.id.rwy_loc_label)
            tv!!.visibility = View.GONE
            val layout = findViewById<LinearLayout>(R.id.rwy_loc_details)
            layout!!.visibility = View.GONE
        }
    }

    private fun showGlideslopeDetails(result: Array<Cursor?>) {
        val ils3 = result[3]
        if (ils3!!.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.rwy_gs_details)
            val gsType = ils3.getString(ils3.getColumnIndex(Ils3.GLIDE_SLOPE_TYPE))
            addRow(layout!!, "Type", gsType)
            val gsAngle = ils3.getFloat(ils3.getColumnIndex(Ils3.GLIDE_SLOPE_ANGLE))
            addRow(layout, "Glide angle", FormatUtils.formatDegrees(gsAngle))
            val gsFreq = ils3.getString(ils3.getColumnIndex(Ils3.GLIDE_SLOPE_FREQUENCY))
            addRow(layout, "Frequency", gsFreq)
            val status = ils3.getString(ils3.getColumnIndex(Ils2.OPERATIONAL_STATUS))
            val date = ils3.getString(ils3.getColumnIndex(Ils2.OPERATIONAL_EFFECTIVE_DATE))
            addRow(layout, "Status", status, date)
        } else {
            val tv = findViewById<TextView>(R.id.rwy_gs_label)
            tv!!.visibility = View.GONE
            val layout = findViewById<LinearLayout>(R.id.rwy_gs_details)
            layout!!.visibility = View.GONE
        }
    }

    private fun showInnerMarkerDetails(result: Array<Cursor?>) {
        val ils5 = result[5]
        if (ils5!!.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.rwy_im_details)
            val imType = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_TYPE))
            addRow(layout!!, "Type", imType)
            val distance = ils5.getInt(ils5.getColumnIndex(Ils5.MARKER_DISTANCE))
            addRow(layout, "Distance", FormatUtils.formatFeet(distance.toFloat()))
            val status = ils5.getString(ils5.getColumnIndex(Ils2.OPERATIONAL_STATUS))
            val date = ils5.getString(ils5.getColumnIndex(Ils2.OPERATIONAL_EFFECTIVE_DATE))
            addRow(layout, "Status", status, date)
        } else {
            val tv = findViewById<TextView>(R.id.rwy_im_label)
            tv!!.visibility = View.GONE
            val layout = findViewById<LinearLayout>(R.id.rwy_im_details)
            layout!!.visibility = View.GONE
        }
    }

    private fun showMiddleMarkerDetails(result: Array<Cursor?>) {
        val ils5 = result[6]
        if (ils5!!.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.rwy_mm_details)
            val mmType = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_TYPE))
            addRow(layout!!, "Type", mmType)
            val mmId = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_BEACON_ID))
            if (mmId.isNotEmpty()) {
                addRow(layout, "Id", mmId)
            }
            val mmName = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_BEACON_NAME))
            if (mmName.isNotEmpty()) {
                addRow(layout, "Name", mmName)
            }
            val mmFreq = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_BEACON_FREQUENCY))
            if (mmFreq.isNotEmpty()) {
                addRow(layout, "Frequency", mmFreq)
            }
            val mmDistance = ils5.getInt(ils5.getColumnIndex(Ils5.MARKER_DISTANCE))
            addRow(layout, "Distance", FormatUtils.formatFeet(mmDistance.toFloat()))
            val status = ils5.getString(ils5.getColumnIndex(Ils2.OPERATIONAL_STATUS))
            val date = ils5.getString(ils5.getColumnIndex(Ils2.OPERATIONAL_EFFECTIVE_DATE))
            addRow(layout, "Status", status, date)
        } else {
            val tv = findViewById<TextView>(R.id.rwy_mm_label)
            tv!!.visibility = View.GONE
            val layout = findViewById<LinearLayout>(R.id.rwy_mm_details)
            layout!!.visibility = View.GONE
        }
    }

    private fun showOuterMarkerDetails(result: Array<Cursor?>) {
        val ils5 = result[7]
        if (ils5!!.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.rwy_om_details)
            val omType = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_TYPE))
            addRow(layout!!, "Type", omType)
            val omId = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_BEACON_ID))
            if (omId.isNotEmpty()) {
                addRow(layout, "Id", omId)
            }
            val omName = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_BEACON_NAME))
            if (omName.isNotEmpty()) {
                addRow(layout, "Name", omName)
            }
            val omFreq = ils5.getString(ils5.getColumnIndex(Ils5.MARKER_BEACON_FREQUENCY))
            if (omFreq.isNotEmpty()) {
                addRow(layout, "Frequency", omFreq)
            }
            val omDistance = ils5.getInt(ils5.getColumnIndex(Ils5.MARKER_DISTANCE))
            addRow(layout, "Distance", FormatUtils.formatFeet(omDistance.toFloat()))
            val status = ils5.getString(ils5.getColumnIndex(Ils2.OPERATIONAL_STATUS))
            val date = ils5.getString(ils5.getColumnIndex(Ils2.OPERATIONAL_EFFECTIVE_DATE))
            addRow(layout, "Status", status, date)
        } else {
            val tv = findViewById<TextView>(R.id.rwy_om_label)
            tv!!.visibility = View.GONE
            val layout = findViewById<LinearLayout>(R.id.rwy_om_details)
            layout!!.visibility = View.GONE
        }
    }

    private fun showIlsRemarks(result: Array<Cursor?>) {
        val ils6 = result[8]
        val layout = findViewById<LinearLayout>(R.id.rwy_ils_remarks)
        if (ils6!!.moveToFirst()) {
            do {
                val remark = ils6.getString(ils6.getColumnIndex(Ils6.ILS_REMARKS))
                addBulletedRow(layout!!, remark)
            } while (ils6.moveToNext())
        } else {
            layout!!.visibility = View.GONE
        }
    }

    private fun doQuery(siteNumber: String, runwayId: String, ilsType: String): Array<Cursor?> {
        val db = getDatabase(DB_FADDS)
        val cursors = arrayOfNulls<Cursor>(9)
        val apt = getAirportDetails(siteNumber)
        cursors[0] = apt
        var builder = SQLiteQueryBuilder()
        builder.tables = Ils1.TABLE_NAME
        cursors[1] = builder.query(
            db,
            arrayOf("*"),
            Ils1.SITE_NUMBER + "=? AND " + Ils1.RUNWAY_ID + "=? AND " + Ils1.ILS_TYPE + "=?",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils2.TABLE_NAME
        cursors[2] = builder.query(
            db,
            arrayOf("*"),
            Ils2.SITE_NUMBER + "=? AND " + Ils2.RUNWAY_ID + "=? AND " + Ils2.ILS_TYPE + "=?",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils3.TABLE_NAME
        cursors[3] = builder.query(
            db,
            arrayOf("*"),
            Ils3.SITE_NUMBER + "=? AND " + Ils3.RUNWAY_ID + "=? AND " + Ils3.ILS_TYPE + "=?",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils4.TABLE_NAME
        cursors[4] = builder.query(
            db,
            arrayOf("*"),
            Ils4.SITE_NUMBER + "=? AND " + Ils4.RUNWAY_ID + "=? AND " + Ils4.ILS_TYPE + "=?",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils5.TABLE_NAME
        cursors[5] = builder.query(
            db,
            arrayOf("*"),
            Ils5.SITE_NUMBER + "=? AND " + Ils5.RUNWAY_ID + "=? AND " + Ils5.ILS_TYPE + "=?"
                    + " AND " + Ils5.MARKER_TYPE + "='IM'",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils5.TABLE_NAME
        cursors[6] = builder.query(
            db,
            arrayOf("*"),
            Ils5.SITE_NUMBER + "=? AND " + Ils5.RUNWAY_ID + "=? AND " + Ils5.ILS_TYPE + "=?"
                    + " AND " + Ils5.MARKER_TYPE + "='MM'",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils5.TABLE_NAME
        cursors[7] = builder.query(
            db,
            arrayOf("*"),
            Ils5.SITE_NUMBER + "=? AND " + Ils5.RUNWAY_ID + "=? AND " + Ils5.ILS_TYPE + "=?"
                    + " AND " + Ils5.MARKER_TYPE + "='OM'",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Ils6.TABLE_NAME
        cursors[8] = builder.query(
            db,
            arrayOf("*"),
            Ils6.SITE_NUMBER + "=? AND " + Ils6.RUNWAY_ID + "=? AND " + Ils6.ILS_TYPE + "=?",
            arrayOf(siteNumber, runwayId, ilsType),
            null,
            null,
            null,
            null
        )
        return cursors
    }
}
