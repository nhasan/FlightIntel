/*
 * FlightIntel for Pilots
 *
 * Copyright 2018-2026 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.LocationListFragmentBase
import com.nadmm.airports.data.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NearbyObstaclesFragment : LocationListFragmentBase() {
    private val mRadius = RADIUS_NM
    // Shared with the Map tab, which draws the obstacles this list finds
    private val viewModel: NearbyObstaclesViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEmptyText("No obstacles found nearby.")
    }

    override fun startLocationTask() {
        val location = lastLocation ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val (cursor, obstacles) = withContext(Dispatchers.IO) {
                val c = doQuery()[0]
                val list = ArrayList<DofListDataModel>(c.count)
                if (c.moveToFirst()) {
                    do {
                        list.add(DofListDataModel.fromCursor(c))
                    } while (c.moveToNext())
                }
                c to list
            }
            setCursor(cursor)
            viewModel.publish(NearbyObstacles(location, mRadius, obstacles))
        }
    }

    override fun newListAdapter(cursor: Cursor?): RecyclerView.Adapter<*>? {
        return cursor?.let { DofRecyclerViewAdapter(it) }
    }

    private fun doQuery(): Array<Cursor> {
        val db = getDatabase(DatabaseManager.DB_DOF)
        val c: Cursor = NearbyDofCursor(db, lastLocation!!, mRadius)
        return arrayOf(c)
    }

    companion object {
        const val RADIUS_NM = 5
    }
}
