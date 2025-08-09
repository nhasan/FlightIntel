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

class NearbyWxFragment : LocationListFragmentBase() {
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
        return listAdapter != null && listAdapter!!.count > 0
    }

    override fun requestDataRefresh() {
        mDelegate?.requestMetars(NoaaService.ACTION_GET_METAR, true, true)
    }

    override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        return mDelegate?.newListAdapter(context, c)
    }

    override fun setCursor(c: Cursor) {
        mDelegate?.let { delegate ->
            delegate.setCursor(c)
            super.setCursor(c)
            activityBase.enableDisableSwipeRefresh(isRefreshable)
            delegate.requestMetars(NoaaService.ACTION_GET_METAR, false, true)
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        mDelegate?.onListItemClick(l, v, position)
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
