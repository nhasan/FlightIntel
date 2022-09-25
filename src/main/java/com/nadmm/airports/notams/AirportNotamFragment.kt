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
package com.nadmm.airports.notams

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Airports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AirportNotamFragment : NotamFragmentBase() {
    private var mfaaCode: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.airport_notam_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.getString(Airports.SITE_NUMBER)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                requestNotams(result[0])
            }
        }
    }

    private fun requestNotams(c: Cursor?) {
        mfaaCode = c!!.getString(c.getColumnIndexOrThrow(Airports.FAA_CODE))
        setActionBarTitle("$mfaaCode - NOTAMs")
        getNotams(mfaaCode, false)
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        getNotams(mfaaCode, true)
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val c = getAirportDetails(siteNumber)
        return arrayOf(c)
    }
}