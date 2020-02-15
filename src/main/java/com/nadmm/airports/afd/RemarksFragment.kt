/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2020 Nadeem Hasan <nhasan@nadmm.com>
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

class RemarksFragment : FragmentBase() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.remarks_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setActionBarTitle("Remarks", "")

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
        showRemarksDetails(result)

        setFragmentContentShown(true)
    }

    private fun showRemarksDetails(result: Array<Cursor?>) {
        val rmk = result[1] ?: return
        val layout = findViewById<LinearLayout>(R.id.detail_remarks_layout)
        if (rmk.moveToFirst()) {
            do {
                val remark = rmk.getString(rmk.getColumnIndex(Remarks.REMARK_TEXT))
                addBulletedRow(layout!!, remark)
            } while (rmk.moveToNext())
        } else {
            addBulletedRow(layout!!, "No airport remarks available")
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(2)

        val apt = getAirportDetails(siteNumber)
        cursors[0] = apt

        val db = getDatabase(DB_FADDS)
        val builder = SQLiteQueryBuilder()
        builder.tables = Remarks.TABLE_NAME
        cursors[1] = builder.query(db, arrayOf(Remarks.REMARK_TEXT),
            "${Runways.SITE_NUMBER}=? "
                + "AND substr(${Remarks.REMARK_NAME}, 1, 2) not in ('A3', 'A4', 'A5', 'A6') "
                + "AND substr(${Remarks.REMARK_NAME}, 1, 3) not in ('A23', 'A17', 'A81') "
                + "AND ${Remarks.REMARK_NAME} "
                + "not in ('E147', 'A3', 'A11', 'A12', 'A13', 'A14', 'A15', 'A16', 'A17', "
                + "'A24', 'A70', 'A75', 'A82')",
            arrayOf(siteNumber), null, null, Remarks.REMARK_NAME, null)

        return cursors
    }

}
