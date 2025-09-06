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

import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import com.nadmm.airports.RecyclerViewFragment
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.utils.NetworkUtils

class WxDelegate(private val fragment: RecyclerViewFragment) {
    private val stationIds = ArrayList<String>()
    private val metarServiceFilter = IntentFilter()

    init {
        metarServiceFilter.addAction(NoaaService.ACTION_GET_METAR)
    }

    fun setCursor(c: Cursor) {
        stationIds.clear()
        try {
            if (c.moveToFirst()) {
                do {
                    val stationId = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
                    stationIds.add(stationId)
                } while (c.moveToNext())
            }
        } catch (_: Exception) {
        }
    }

    fun requestMetars(force: Boolean) {
        val activity = fragment.activityBase
        if (stationIds.isEmpty()) {
            activity.isRefreshing = false
            return
        }
        val cacheOnly = !NetworkUtils.canDownloadData(activity)
        activity.isRefreshing = force

        val service = Intent(activity, MetarService::class.java)
        service.action = NoaaService.ACTION_GET_METAR
        service.putExtra(NoaaService.STATION_IDS, stationIds)
        service.putExtra(NoaaService.TYPE, NoaaService.TYPE_TEXT)
        service.putExtra(NoaaService.FORCE_REFRESH, force)
        service.putExtra(NoaaService.CACHE_ONLY, cacheOnly)
        try {
            activity.startService(service)
        } catch (e: Exception) {
            Log.i("WxDelegate","Exception: $e")
        }
    }

    fun newListAdapter(cursor: Cursor?): WxRecyclerViewAdapter? {
        return cursor?.let { WxRecyclerViewAdapter(it, ::onRecyclerItemClick) }
    }

    fun onRecyclerItemClick(model: WxListDataModel) {
        val intent = Intent(fragment.context, WxDetailActivity::class.java).apply {
            putExtras(Bundle().apply { putString(NoaaService.STATION_ID, model.stationId) })
        }
        fragment.startActivity(intent)
    }
}