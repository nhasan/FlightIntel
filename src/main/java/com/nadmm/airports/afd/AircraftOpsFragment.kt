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

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.R.layout
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.utils.CursorAsyncTask
import com.nadmm.airports.utils.FormatUtils

class AircraftOpsFragment : FragmentBase() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View? = inflater.inflate(layout.aircraft_ops_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("Operations", "")
        val siteNumber = arguments!!.getString(Airports.SITE_NUMBER)
        setBackgroundTask(OpsDetailsTask(this)).execute(siteNumber)
    }

    protected fun showDetails(result: Array<Cursor?>) {
        val apt = result[0]?: return
        showAirportTitle(apt)
        showBasedAircraft(apt)
        showAnnualOps(apt)
        setFragmentContentShown(true)
    }

    private fun showBasedAircraft(apt: Cursor) {
        val layout: LinearLayout = findViewById(R.id.based_aircraft_layout)
        var count = apt.getInt(apt.getColumnIndex(Airports.SINGLE_ENGINE_COUNT))
        addRow(layout, "Single engine", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.MULTI_ENGINE_COUNT))
        addRow(layout, "Multi engine", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.JET_ENGINE_COUNT))
        addRow(layout, "Jet engine", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.HELI_COUNT))
        addRow(layout, "Helicopters", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.GLIDERS_COUNT))
        addRow(layout, "Gliders", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.ULTRA_LIGHT_COUNT))
        addRow(layout, "Ultra light", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.MILITARY_COUNT))
        addRow(layout, "Military", FormatUtils.formatNumber(count.toFloat()))
    }

    private fun showAnnualOps(apt: Cursor) {
        val layout: LinearLayout = findViewById(R.id.aircraft_ops_layout)
        var count = apt.getInt(apt.getColumnIndex(Airports.ANNUAL_COMMERCIAL_OPS))
        addRow(layout, "Commercial", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.ANNUAL_COMMUTER_OPS))
        addRow(layout, "Commuter", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.ANNUAL_AIRTAXI_OPS))
        addRow(layout, "Air taxi", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.ANNUAL_GA_LOCAL_OPS))
        addRow(layout, "GA local", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.ANNUAL_GA_ININERANT_OPS))
        addRow(layout, "GA other", FormatUtils.formatNumber(count.toFloat()))
        count = apt.getInt(apt.getColumnIndex(Airports.ANNUAL_MILITARY_OPS))
        addRow(layout, "Military", FormatUtils.formatNumber(count.toFloat()))
        val date: String? = apt.getString(apt.getColumnIndex(Airports.OPS_REFERENCE_DATE))
        addRow(layout, "As-of", date)
    }

    private class OpsDetailsTask(fragment: AircraftOpsFragment)
        : CursorAsyncTask<AircraftOpsFragment>(fragment) {

        override fun onExecute(fragment: AircraftOpsFragment, vararg params: String)
                : Array<Cursor?> {
            val siteNumber = params[0]
            val apt: Cursor = fragment.getAirportDetails(siteNumber)
            return arrayOf(apt)
        }

        override fun onResult(fragment: AircraftOpsFragment, result: Array<Cursor?>)
                : Boolean {
            fragment.showDetails(result)
            return true
        }
    }
}