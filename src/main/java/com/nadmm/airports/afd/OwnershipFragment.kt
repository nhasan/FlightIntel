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
import com.nadmm.airports.utils.DataUtils.decodeFacilityUse
import com.nadmm.airports.utils.DataUtils.decodeOwnershipType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("Range")
class OwnershipFragment : FragmentBase() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ownership_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("Ownership info", "")

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
        showOwnershipType(result)
        showOwnerInfo(result)
        showManagerInfo(result)
        showRemarks(result)
        setFragmentContentShown(true)
    }

    private fun showOwnershipType(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        val layout = findViewById<LinearLayout>(R.id.detail_ownership_type_layout)
        val ownership = decodeOwnershipType(
            apt.getString(apt.getColumnIndex(Airports.OWNERSHIP_TYPE))
        )
        val use = decodeFacilityUse(
            apt.getString(apt.getColumnIndex(Airports.FACILITY_USE))
        )
        addSimpleRow(layout!!, "$ownership / $use")
    }

    private fun showOwnerInfo(result: Array<Cursor?>) {
        val apt = result[0]
        var layout = findViewById<LinearLayout>(R.id.detail_owner_layout)
        var text: String = apt!!.getString(apt.getColumnIndex(Airports.OWNER_NAME))
        addSimpleRow(layout!!, text)
        text = apt.getString(apt.getColumnIndex(Airports.OWNER_ADDRESS))
        addSimpleRow(layout, text)
        text = apt.getString(apt.getColumnIndex(Airports.OWNER_CITY_STATE_ZIP))
        addSimpleRow(layout, text)
        layout = findViewById(R.id.detail_owner_phone_layout)
        text = apt.getString(apt.getColumnIndex(Airports.OWNER_PHONE))
        if (text.isNotEmpty()) {
            addPhoneRow(layout!!, text)
        } else {
            layout!!.visibility = View.GONE
            findViewById<View>(R.id.detail_owner_phone_label)!!.visibility = View.GONE
        }
    }

    private fun showManagerInfo(result: Array<Cursor?>) {
        val apt = result[0]
        var layout = findViewById<LinearLayout>(R.id.detail_manager_layout)
        var text: String = apt!!.getString(apt.getColumnIndex(Airports.MANAGER_NAME))
        addSimpleRow(layout!!, text)
        text = apt.getString(apt.getColumnIndex(Airports.MANAGER_ADDRESS))
        addSimpleRow(layout, text)
        text = apt.getString(apt.getColumnIndex(Airports.MANAGER_CITY_STATE_ZIP))
        addSimpleRow(layout, text)
        layout = findViewById(R.id.detail_manager_phone_layout)
        text = apt.getString(apt.getColumnIndex(Airports.MANAGER_PHONE))
        if (text.isNotEmpty()) {
            addPhoneRow(layout!!, text)
        } else {
            layout!!.visibility = View.GONE
            findViewById<View>(R.id.detail_manager_phone_label)!!.visibility = View.GONE
        }
    }

    private fun showRemarks(result: Array<Cursor?>) {
        val rmk = result[1]
        if (rmk == null || !rmk.moveToFirst()) {
            return
        }
        val layout = findViewById<LinearLayout>(R.id.detail_remarks_layout)
        layout!!.visibility = View.VISIBLE
        do {
            val remark = rmk.getString(rmk.getColumnIndex(Remarks.REMARK_TEXT))
            addBulletedRow(layout, remark)
        } while (rmk.moveToNext())
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val db = getDatabase(DB_FADDS)
        val cursors = arrayOfNulls<Cursor>(2)
        cursors[0] = getAirportDetails(siteNumber)
        val builder = SQLiteQueryBuilder()
        builder.tables = Remarks.TABLE_NAME
        cursors[1] = builder.query(
            db,
            arrayOf(Remarks.REMARK_TEXT),
            "${Remarks.SITE_NUMBER}=? AND ${Remarks.REMARK_NAME} in " +
                    "('A11', 'A12', 'A13', 'A14', 'A15', 'A16')",
            arrayOf(siteNumber),
            null,
            null,
            null,
            null
        )
        return cursors
    }

}