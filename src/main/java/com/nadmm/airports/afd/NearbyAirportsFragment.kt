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

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.LocationListFragment2
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NearbyAirportsFragment : LocationListFragment2() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setEmptyText("No airports found nearby.")

        if (!isLocationUpdateEnabled) {
            setActionBarTitle("Nearby Airports")
            setActionBarSubtitle("Within $nearbyRadius NM radius")
        }
    }

    override fun startLocationTask() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                doQuery()
            }
            setCursor(result[0])
        }
    }

    override fun newListAdapter(cursor: Cursor?): AirportsRecyclerViewAdapter? {
        return if (cursor != null) {
            AirportsRecyclerViewAdapter(cursor, ::onRecyclerItemClick)
        } else {
            null
        }
    }

    private fun onRecyclerItemClick(model: AirportListDataModel) {
        Intent(activity, AirportActivity::class.java).apply {
            putExtras(model.makeBundle())
            startActivity(this)
        }
    }

    private fun doQuery(): Array<Cursor> {
        val db = getDatabase(DatabaseManager.DB_FADDS)

        val extraSelection = arguments?.getString(Airports.SITE_NUMBER)?.let {
            "AND ${Airports.SITE_NUMBER} <> '$it'"
        }

        val c = NearbyAirportsCursor(db, lastLocation!!, nearbyRadius, extraSelection)
        return arrayOf(c)
    }

}
