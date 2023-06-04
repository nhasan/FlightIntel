/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2023 Nadeem Hasan <nhasan@nadmm.com>
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
import androidx.cursoradapter.widget.CursorAdapter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.ListFragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.utils.NetworkUtils

class WxDelegate(private val mFragment: ListFragmentBase) {
    private val mStationWx = HashMap<String, Metar?>()
    private val mReceiver: BroadcastReceiver
    private val mFilter: IntentFilter
    fun onPause() {
        val bm = LocalBroadcastManager.getInstance(mFragment.requireContext())
        bm.unregisterReceiver(mReceiver)
    }

    fun onResume() {
        val bm = LocalBroadcastManager.getInstance(mFragment.requireContext())
        bm.registerReceiver(mReceiver, mFilter)
    }

    fun setCursor(c: Cursor) {
        mStationWx.clear()
        try {
            if (c.moveToFirst()) {
                do {
                    val stationId = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
                    mStationWx[stationId] = null
                } while (c.moveToNext())
            }
        } catch (_: Exception) {
        }
    }

    fun requestMetars(action: String, force: Boolean, showAnim: Boolean) {
        val activity = mFragment.activityBase
        if (mStationWx.isEmpty()) {
            activity.isRefreshing = false
            return
        }
        val cacheOnly = !NetworkUtils.canDownloadData(activity)
        if ((force || !cacheOnly) && showAnim) {
            activity.isRefreshing = true
        }
        val stationIds = ArrayList(mStationWx.keys)
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
        // When getting wx for one station, get for all others too
        try {
            requestMetars(NoaaService.ACTION_CACHE_METAR, force = false, showAnim = false)
            val c = l.getItemAtPosition(position) as Cursor
            val icaoCode = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
            val intent = Intent(mFragment.context, WxDetailActivity::class.java)
            val args = Bundle()
            args.putString(NoaaService.STATION_ID, icaoCode)
            intent.putExtras(args)
            mFragment.startActivity(intent)
        } catch (_: Exception) {
        }

    fun getMetar(stationId: String): Metar? {
        return mStationWx[stationId]
    }

    private inner class WxReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val metar = intent.getSerializableExtra(NoaaService.RESULT) as? Metar ?: return
                val stationId = metar.stationId ?: return
                mStationWx[stationId] = metar
                val l = mFragment.findViewById<ListView>(android.R.id.list) ?: return
                val first = l.firstVisiblePosition
                for (pos in 0 until l.childCount) {
                    val view = l.getChildAt(pos)
                    val icaoCode = view.getTag(R.id.TAG_STATION_ID) as? String ?: continue
                    if (icaoCode == stationId) {
                        val adapter = mFragment.listAdapter as? WxCursorAdapter ?: continue
                        val c = adapter.getItem(pos + first) as Cursor
                        if (c.position >= 0) {
                            adapter.showMetarInfo(view, c, metar)
                        }
                        break
                    }
                }
                if (mFragment.isRefreshing) {
                    mFragment.isRefreshing = false
                }
            } catch (e: Exception) {
                Log.i("WxDelegate","Exception: ${e.message}")
            }
        }
    }

    init {
        mReceiver = WxReceiver()
        mFilter = IntentFilter()
        mFilter.addAction(NoaaService.ACTION_GET_METAR)
    }
}