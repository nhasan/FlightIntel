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
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("Range")
class AttendanceFragment : FragmentBase() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.attendance_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setActionBarTitle("Attendance", "")

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
        val apt = result[0] ?: return

        showAirportTitle(apt)
        showAttendanceDetails(result)
        showAttendanceRemarks(result)

        setFragmentContentShown(true)
    }

    private fun showAttendanceDetails(result: Array<Cursor?>) {
        val att = result[1] ?: return
        if (att.moveToFirst()) {
            val layout = findViewById<LinearLayout>(R.id.attendance_content_layout)
            do {
                val schedule = att.getString(
                        att.getColumnIndex(Attendance.ATTENDANCE_SCHEDULE))
                val parts = schedule.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val item = inflate<LinearLayout>(R.layout.attendance_detail_item,
                        layout!!)
                if (parts.size == 3) {
                    addRow(item, "Months", parts[0])
                    addRow(item, "Days", parts[1])
                    addRow(item, "Hours", parts[2])
                } else {
                    addRow(item, "Attendance", schedule)
                }
                layout.addView(item)
            } while (att.moveToNext())
        }
    }

    private fun showAttendanceRemarks(result: Array<Cursor?>) {
        val rmk = result[2] ?: return
        val layout = findViewById<LinearLayout>(R.id.attendance_remark_layout)
        if (rmk.moveToFirst()) {
            layout!!.visibility = View.VISIBLE
            do {
                val remark = rmk.getString(rmk.getColumnIndex(Remarks.REMARK_TEXT))
                addBulletedRow(layout, remark)
            } while (rmk.moveToNext())
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(3)

        cursors[0] = getAirportDetails(siteNumber)

        val db = getDatabase(DB_FADDS)
        var builder = SQLiteQueryBuilder()
        builder.tables = Attendance.TABLE_NAME
        cursors[1] = builder.query(db,
                arrayOf(Attendance.ATTENDANCE_SCHEDULE),
                Attendance.SITE_NUMBER + "=?", arrayOf(siteNumber), null, null, Attendance.SEQUENCE_NUMBER, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Remarks.TABLE_NAME
        cursors[2] = builder.query(db,
                arrayOf(Remarks.REMARK_TEXT),
                Attendance.SITE_NUMBER + "=? "
                        + "AND substr(" + Remarks.REMARK_NAME + ", 1, 3)='A17'",
                arrayOf(siteNumber), null, null, null, null)

        return cursors
    }

}
