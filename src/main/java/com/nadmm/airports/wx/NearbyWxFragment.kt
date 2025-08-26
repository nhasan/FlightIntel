/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.database.Cursor
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.LocationListFragment2
import com.nadmm.airports.data.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NearbyWxFragment : LocationListFragment2() {
    private var mDelegate: WxDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDelegate = WxDelegate(this)
    }

    override fun onResume() {
        super.onResume()

        mDelegate?.onResume()
    }

    override fun onPause() {
        super.onPause()

        mDelegate?.onPause()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setEmptyText("No wx stations found nearby.")
    }

    override fun isRefreshable(): Boolean {
        val adapter = recyclerView.adapter as? WxRecyclerAdapter
        return adapter != null && adapter.itemCount > 0
    }

    override fun requestDataRefresh() {
        mDelegate?.requestMetars(true)
    }

    override fun newListAdapter(cursor: Cursor?): RecyclerView.Adapter<*>? {
        return mDelegate?.newListAdapter(cursor)
    }

    override fun setCursor(cursor: Cursor) {
        mDelegate?.let { delegate ->
            delegate.setCursor(cursor)
            super.setCursor(cursor)
            activityBase.enableDisableSwipeRefresh(isRefreshable)
            delegate.requestMetars(false)
        }
    }

    private fun doQuery(): Array<Cursor> {
        val db = getDatabase(DatabaseManager.DB_FADDS)
        val c: Cursor = NearbyWxCursor(db, lastLocation!!, nearbyRadius)
        return arrayOf(c)
    }

    override fun startLocationTask() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                doQuery()
            }
            setCursor(result[0])
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDelegate = null
    }
}
