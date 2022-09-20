/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.tfr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.AbsListView
import android.widget.ListView
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.ListFragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.TimeUtils
import java.util.*

class TfrListFragment : ListFragmentBase() {
    private var mReceiver: BroadcastReceiver? = null
    private var mFilter: IntentFilter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mReceiver = TfrReceiver()
        mFilter = IntentFilter()
        mFilter!!.addAction(TfrService.ACTION_GET_TFR_LIST)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val listView = listView!!
        val footer = inflater.inflate(R.layout.tfr_list_footer_view, listView, false)
        footer.layoutParams = AbsListView.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val tv = footer.findViewById<TextView>(R.id.tfr_warning_text)
        tv.text = ("Depicted TFR data may not be a complete listing. Pilots should not use "
                + "the information for flight planning purposes. For the latest information, "
                + "call your local Flight Service Station at 1-800-WX-BRIEF.")
        listView.addFooterView(footer, null, false)
        listView.setFooterDividersEnabled(true)
        return view
    }

    override fun onResume() {
        val bm = LocalBroadcastManager.getInstance(activity!!)
        bm.registerReceiver(mReceiver!!, mFilter!!)
        super.onResume()
    }

    override fun onPause() {
        val bm = LocalBroadcastManager.getInstance(activity!!)
        bm.unregisterReceiver(mReceiver!!)
        super.onPause()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("TFR List")
        setActionBarSubtitle("Loading...")
        requestTfrList(false)
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        requestTfrList(true)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        val adapter = listView!!.adapter
        val tfr = adapter.getItem(position) as Tfr?
        if (tfr != null) {
            val activity = Intent(activity, TfrDetailActivity::class.java)
            activity.putExtra(TfrListActivity.EXTRA_TFR, tfr)
            startActivity(activity)
        }
    }

    private fun requestTfrList(force: Boolean) {
        val service = Intent(activity, TfrService::class.java)
        service.action = TfrService.ACTION_GET_TFR_LIST
        service.putExtra(TfrService.FORCE_REFRESH, force)
        requireActivity().startService(service)
    }

    private inner class TfrReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val tfrList = intent.getSerializableExtra(TfrService.TFR_LIST) as TfrList?
            val count = tfrList!!.entries.size
            if (count > 0) {
                val tv = findViewById<TextView>(R.id.tfr_fetch_time)
                tv!!.text = "Fetched " + TimeUtils.formatElapsedTime(
                    tfrList.fetchTime
                )
                setActionBarSubtitle(String.format(Locale.US, "%d TFRs found", count))
            } else {
                setEmptyText("Unable to fetch TFR list. Please try again later")
                setActionBarSubtitle("")
            }
            val adapter = TfrListAdapter(activityBase, tfrList)
            setAdapter(adapter)
            isRefreshing = false
        }
    }
}