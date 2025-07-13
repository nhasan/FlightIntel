/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.databinding.AircraftOpsViewBinding
import com.nadmm.airports.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AircraftOpsFragment : FragmentBase() {

    private var _binding: AircraftOpsViewBinding? = null;
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = AircraftOpsViewBinding.inflate(inflater, container, false)
        val view = binding.root
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setActionBarTitle("Operations", "")

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
        val apt = result[0]?: return
        showAirportTitle(apt, binding.airportTitle)
        showBasedAircraft(apt)
        showAnnualOps(apt)
        setFragmentContentShown(true)
    }

    private fun showBasedAircraft(apt: Cursor) {
        with (binding.basedAircraftLayout) {
            var count = apt.getInt(apt.getColumnIndexOrThrow(Airports.SINGLE_ENGINE_COUNT))
            addRow(this, "Single engine", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.MULTI_ENGINE_COUNT))
            addRow(this, "Multi engine", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.JET_ENGINE_COUNT))
            addRow(this, "Jet engine", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.HELI_COUNT))
            addRow(this, "Helicopters", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.GLIDERS_COUNT))
            addRow(this, "Gliders", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ULTRA_LIGHT_COUNT))
            addRow(this, "Ultra light", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.MILITARY_COUNT))
            addRow(this, "Military", FormatUtils.formatNumber(count.toFloat()))
        }
    }

    private fun showAnnualOps(apt: Cursor) {
        with (binding.aircraftOpsLayout) {
            var count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ANNUAL_COMMERCIAL_OPS))
            addRow(this, "Commercial", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ANNUAL_COMMUTER_OPS))
            addRow(this, "Commuter", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ANNUAL_AIRTAXI_OPS))
            addRow(this, "Air taxi", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ANNUAL_GA_LOCAL_OPS))
            addRow(this, "GA local", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ANNUAL_GA_ININERANT_OPS))
            addRow(this, "GA other", FormatUtils.formatNumber(count.toFloat()))
            count = apt.getInt(apt.getColumnIndexOrThrow(Airports.ANNUAL_MILITARY_OPS))
            addRow(this, "Military", FormatUtils.formatNumber(count.toFloat()))
            val date: String? = apt.getString(apt.getColumnIndexOrThrow(Airports.OPS_REFERENCE_DATE))
            addRow(this, "As-of", date)
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        return try {
            val c = getAirportDetails(siteNumber)
            arrayOf(c)
        } catch (_: Exception) {
            // Handle exception
            arrayOfNulls(1)
        }
    }

}