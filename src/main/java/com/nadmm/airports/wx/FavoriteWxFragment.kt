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
import com.nadmm.airports.RecyclerViewFragment
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Wxs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteWxFragment : RecyclerViewFragment() {
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

    override fun requestDataRefresh() {
        mDelegate.requestMetars(true)
    }

    public override fun setCursor(cursor: Cursor) {
        mDelegate.setCursor(cursor)
        super.setCursor(cursor)
        mDelegate.requestMetars(false)
        activityBase.enableDisableSwipeRefresh(isRefreshable)
    }

    override fun newListAdapter(cursor: Cursor?): RecyclerView.Adapter<*>? {
        return mDelegate.newListAdapter(cursor)
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