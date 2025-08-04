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
import com.nadmm.airports.ListFragmentBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Wxs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteWxFragment : ListFragmentBase() {
    private val mDelegate = WxDelegate(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)
    }

    override fun onResume() {
        super.onResume()

        mDelegate.onResume()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                doQuery()
            }
            setCursor(result[0])
        }
    }

    override fun onPause() {
        super.onPause()

        mDelegate.onPause()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setEmptyText("No favorite wx stations selected.")
    }

    override fun isRefreshable(): Boolean {
        return listAdapter != null && !listAdapter!!.isEmpty()
    }

    override fun requestDataRefresh() {
        mDelegate.requestMetars(NoaaService.ACTION_GET_METAR, true, true)
    }

    public override fun setCursor(c: Cursor) {
        mDelegate.setCursor(c)
        super.setCursor(c)
        mDelegate.requestMetars(NoaaService.ACTION_GET_METAR, false, true)
        activityBase.enableDisableSwipeRefresh(isRefreshable)
    }

    override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        return mDelegate.newListAdapter(context, c)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        mDelegate.onListItemClick(l, v, position)
    }

    private fun doQuery(): Array<Cursor> {
        val dbManager = dbManager
        val favorites = dbManager.wxFavorites

        val builder = StringBuilder()
        for (stationId in favorites) {
            if (builder.isNotEmpty()) {
                builder.append(", ")
            }
            builder.append("'").append(stationId).append("'")
        }

        val db = getDatabase(DatabaseManager.DB_FADDS)
        val selection = Wxs.STATION_ID + " in (" + builder.toString() + ")"
        val c = WxCursorHelper.query(
            db, selection, null, null, null,
            Wxs.STATION_NAME, null
        )
        return arrayOf(c)
    }
}