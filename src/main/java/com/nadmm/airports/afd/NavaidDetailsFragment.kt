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
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.notams.NavaidNotamActivity
import com.nadmm.airports.utils.DataUtils.decodeNavProtectedAltitude
import com.nadmm.airports.utils.DataUtils.getTacanChannelFrequency
import com.nadmm.airports.utils.DataUtils.isDirectionalNavaid
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@SuppressLint("Range")
class NavaidDetailsFragment : FragmentBase() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.navaid_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("Navaid details", "")

        arguments?.let {
            val navaidId = it.getString(Nav1.NAVAID_ID) ?: ""
            val navaidType = it.getString(Nav1.NAVAID_TYPE) ?: ""

            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(navaidId, navaidType)
                }
                showDetails(result)
            }
        }
    }

    private fun showDetails(result: Array<Cursor?>?) {
        if (result == null) {
            showToast(requireActivity(), "Navaid not found")
            requireActivity().finish()
            return
        }
        val nav1 = result[0]
        showNavaidTitle(nav1!!)
        showNavaidDetails(result)
        showNavaidNotams(result)
        showNavaidRemarks(result)
        setFragmentContentShown(true)
    }

    private fun showNavaidDetails(result: Array<Cursor?>) {
        val nav1 = result[0]
        val layout = findViewById<LinearLayout>(R.id.navaid_details)
        val navaidClass = nav1!!.getString(nav1.getColumnIndex(Nav1.NAVAID_CLASS))
        val navaidType = nav1.getString(nav1.getColumnIndex(Nav1.NAVAID_TYPE))
        addRow(layout!!, "Class", navaidClass)
        var freq = nav1.getDouble(nav1.getColumnIndex(Nav1.NAVAID_FREQUENCY))
        val tacan = nav1.getString(nav1.getColumnIndex(Nav1.TACAN_CHANNEL))
        if (freq == 0.0) {
            freq = getTacanChannelFrequency(tacan)
        }
        if (freq > 0) {
            if (!isDirectionalNavaid(navaidType) && freq % 1.0 == 0.0) {
                addRow(layout, "Frequency", String.format(Locale.US, "%.0f", freq))
            } else {
                addRow(layout, "Frequency", String.format(Locale.US, "%.2f", freq))
            }
        }
        val power = nav1.getString(nav1.getColumnIndex(Nav1.POWER_OUTPUT))
        if (power.isNotEmpty()) {
            addRow(layout, "Power output", "$power Watts")
        }
        if (tacan.isNotEmpty()) {
            addRow(layout, "Tacan channel", tacan)
        }
        val magVar = nav1.getString(nav1.getColumnIndex(Nav1.MAGNETIC_VARIATION_DEGREES))
        if (magVar.isNotEmpty()) {
            val magDir = nav1.getString(
                nav1.getColumnIndex(
                    Nav1.MAGNETIC_VARIATION_DIRECTION
                )
            )
            val magYear = nav1.getString(
                nav1.getColumnIndex(
                    Nav1.MAGNETIC_VARIATION_YEAR
                )
            )
            addRow(
                layout, "Magnetic variation", String.format(
                    Locale.US, "%d\u00B0%s (%s)",
                    Integer.valueOf(magVar), magDir, magYear
                )
            )
        }
        val alt = nav1.getString(nav1.getColumnIndex(Nav1.PROTECTED_FREQUENCY_ALTITUDE))
        if (alt.isNotEmpty()) {
            addRow(layout, "Service volume", decodeNavProtectedAltitude(alt))
        }
        val hours = nav1.getString(nav1.getColumnIndex(Nav1.OPERATING_HOURS))
        addRow(layout, "Operating hours", hours)
        val type = nav1.getString(nav1.getColumnIndex(Nav1.FANMARKER_TYPE))
        if (type.isNotEmpty()) {
            addRow(layout, "Fan marker type", type)
        }
        val voiceFeature = nav1.getString(nav1.getColumnIndex(Nav1.VOICE_FEATURE))
        addRow(layout, "Voice feature", if (voiceFeature == "Y") "Yes" else "No")
        val voiceIdent = nav1.getString(nav1.getColumnIndex(Nav1.AUTOMATIC_VOICE_IDENT))
        addRow(layout, "Voice ident", if (voiceIdent == "Y") "Yes" else "No")
        val com = result[2]
        if (com != null && com.moveToFirst()) {
            do {
                val outletType = com.getString(com.getColumnIndex(Com.COMM_OUTLET_TYPE))
                val fssName = com.getString(com.getColumnIndex(Com.FSS_NAME))
                val freqs = com.getString(com.getColumnIndex(Com.COMM_OUTLET_FREQS))
                val outletName = "$fssName Radio ($outletType)"
                var i = 0
                while (i < freqs.length) {
                    val end = (i + 9).coerceAtMost(freqs.length)
                    val fssFreq = freqs.substring(i, end).trim { it <= ' ' }
                    addRow(layout, outletName, fssFreq)
                    i = end
                }
            } while (com.moveToNext())
        }
    }

    private fun showNavaidNotams(result: Array<Cursor?>) {
        val nav1 = result[0]
        val layout = findViewById<LinearLayout>(R.id.navaid_notams)
        val navaidId = nav1!!.getString(nav1.getColumnIndex(Nav1.NAVAID_ID))
        val navaidType = nav1.getString(nav1.getColumnIndex(Nav1.NAVAID_TYPE))
        val intent = Intent(activity, NavaidNotamActivity::class.java)
        intent.putExtra(Nav1.NAVAID_ID, navaidId)
        intent.putExtra(Nav1.NAVAID_TYPE, navaidType)
        addClickableRow(layout!!, "View NOTAMs", intent)
    }

    private fun showNavaidRemarks(result: Array<Cursor?>) {
        val nav2 = result[1]
        val layout = findViewById<LinearLayout>(R.id.navaid_remarks)
        if (nav2!!.moveToFirst()) {
            do {
                val remark = nav2.getString(nav2.getColumnIndex(Nav2.REMARK_TEXT))
                addBulletedRow(layout!!, remark)
            } while (nav2.moveToNext())
        } else {
            layout!!.visibility = View.GONE
        }
    }

    private fun doQuery(navaidId: String, navaidType: String): Array<Cursor?>? {
        val db = getDatabase(DB_FADDS)
        val cursors = arrayOfNulls<Cursor>(3)
        var builder = SQLiteQueryBuilder()
        builder.tables = (Nav1.TABLE_NAME + " a LEFT OUTER JOIN " + States.TABLE_NAME + " s"
                + " ON a." + Nav1.ASSOC_STATE + "=s." + States.STATE_CODE)
        var c = builder.query(
            db,
            arrayOf("*"),
            Nav1.NAVAID_ID + "=? AND " + Nav1.NAVAID_TYPE + "=?",
            arrayOf(navaidId, navaidType),
            null,
            null,
            null,
            null
        )
        if (!c.moveToFirst()) {
            return null
        }
        cursors[0] = c
        builder = SQLiteQueryBuilder()
        builder.tables = Nav2.TABLE_NAME
        c = builder.query(
            db,
            arrayOf("*"),
            Nav1.NAVAID_ID + "=? AND " + Nav1.NAVAID_TYPE + "=?",
            arrayOf(navaidId, navaidType),
            null,
            null,
            null,
            null
        )
        cursors[1] = c
        if (navaidType != "VOT") {
            builder = SQLiteQueryBuilder()
            builder.tables = Com.TABLE_NAME
            c = builder.query(
                db, arrayOf("*"),
                Com.ASSOC_NAVAID_ID + "=?", arrayOf(navaidId),
                null, null, null, null
            )
            cursors[2] = c
        } else {
            cursors[2] = null
        }
        return cursors
    }
}