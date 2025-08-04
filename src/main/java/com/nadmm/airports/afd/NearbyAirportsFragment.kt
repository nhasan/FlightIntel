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

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.LocationListFragmentBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.utils.makeAirportBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NearbyAirportsFragment : LocationListFragmentBase() {

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

    override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        return AirportsCursorAdapter(context, c)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        Intent(activity, AirportActivity::class.java).apply {
            val c = l.getItemAtPosition(position) as Cursor
            putExtras(c.makeAirportBundle())
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
