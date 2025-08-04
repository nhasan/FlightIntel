/*
 * FlightIntel for Pilots
 *
 * Copyright 2018-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.dof

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.LocationListFragmentBase
import com.nadmm.airports.data.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class NearbyObstaclesFragment : LocationListFragmentBase() {
    private val mRadius = 5

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setEmptyText("No obstacles found nearby.")
        setActionBarTitle("Nearby Obstacles", "")
        setActionBarSubtitle(String.format(Locale.US, "Within %d NM radius", mRadius))
    }

    override fun startLocationTask() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                doQuery()
            }
            setCursor(result[0])
        }
    }

    override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter {
        return DofCursorAdapter(context, c)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {}

    private fun doQuery(): Array<Cursor> {
        val db = getDatabase(DatabaseManager.DB_DOF)
        val c: Cursor = NearbyDofCursor(db, lastLocation!!, mRadius)
        return arrayOf(c)
    }
}