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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import androidx.core.content.IntentCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.ListFragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.utils.NetworkUtils

class WxDelegate(private val fragment: ListFragmentBase) {
    private val stationIds = ArrayList<String>()
    private val stationMetars = HashMap<String, Metar>()
    private val broadcastReceiver = WxReceiver()
    private val metarServiceFilter = IntentFilter()

    init {
        metarServiceFilter.addAction(NoaaService.ACTION_GET_METAR)
    }

    fun onPause() {
        val bm = LocalBroadcastManager.getInstance(fragment.requireContext())
        bm.unregisterReceiver(broadcastReceiver)
    }

    fun onResume() {
        val bm = LocalBroadcastManager.getInstance(fragment.requireContext())
        bm.registerReceiver(broadcastReceiver, metarServiceFilter)
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

    fun requestMetars(action: String, force: Boolean, showAnim: Boolean) {
        val activity = fragment.activityBase
        if (stationIds.isEmpty()) {
            activity.isRefreshing = false
            return
        }
        val cacheOnly = !NetworkUtils.canDownloadData(activity)
        if ((force || !cacheOnly) && showAnim) {
            activity.isRefreshing = true
        }
        val service = Intent(activity, MetarService::class.java)
        service.action = action
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

    fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter {
        return WxCursorAdapter(context, c, this)
    }

    fun onListItemClick(l: ListView, v: View?, position: Int) =
        try {
            val c = l.getItemAtPosition(position) as Cursor
            val icaoCode = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
            val intent = Intent(fragment.context, WxDetailActivity::class.java)
            val args = Bundle()
            args.putString(NoaaService.STATION_ID, icaoCode)
            intent.putExtras(args)
            fragment.startActivity(intent)
        } catch (_: Exception) {
        }

    fun getMetar(stationId: String): Metar? {
        return stationMetars.getOrDefault(stationId, null)
    }

    private inner class WxReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val metar = IntentCompat.getParcelableExtra(intent, NoaaService.RESULT, Metar::class.java) ?: return
                val stationId = metar.stationId ?: return
                stationMetars[stationId] = metar
                val l = fragment.findViewById<ListView>(android.R.id.list) ?: return
                val first = l.firstVisiblePosition
                for (pos in 0 until l.childCount) {
                    val view = l.getChildAt(pos)
                    val icaoCode = view.getTag(R.id.TAG_STATION_ID) as? String ?: continue
                    if (icaoCode == stationId) {
                        val adapter = fragment.listAdapter as? WxCursorAdapter ?: continue
                        val c = adapter.getItem(pos + first) as Cursor
                        if (c.position >= 0) {
                            adapter.showMetarInfo(view, c, metar)
                        }
                        break
                    }
                }
                fragment.isRefreshing = false
            } catch (e: Exception) {
                Log.i("WxDelegate","Exception: ${e.message}")
            }
        }
    }
}