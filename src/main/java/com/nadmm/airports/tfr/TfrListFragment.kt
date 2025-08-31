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
package com.nadmm.airports.tfr

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.IntentCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.databinding.RecyclerViewLayoutBinding
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.UiUtils

class TfrListFragment : FragmentBase() {

    private val mReceiver: BroadcastReceiver
    private val mFilter: IntentFilter
    private var _binding: RecyclerViewLayoutBinding? = null
    private val binding get() = _binding!!
    val recyclerView get() = binding.recyclerView

    init {
        mReceiver = TfrReceiver()
        mFilter = IntentFilter()
        mFilter.addAction(TfrService.ACTION_GET_TFR_LIST)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.registerReceiver(mReceiver, mFilter)
        super.onResume()
    }

    override fun onPause() {
        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.unregisterReceiver(mReceiver)
        super.onPause()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("TFR List")
        setActionBarSubtitle("Loading...")
        requestTfrList(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.adapter = null
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = RecyclerViewLayoutBinding.inflate(inflater, container, false)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        UiUtils.setupWindowInsetsListener(recyclerView)

        return binding.root
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        requestTfrList(true)
    }

    fun onRecyclerItemClick(tfr: Tfr) {
        val activity = Intent(activity, TfrDetailActivity::class.java)
        activity.putExtra(TfrListActivity.EXTRA_TFR, tfr)
        startActivity(activity)
    }

    private fun requestTfrList(force: Boolean) {
        val service = Intent(activity, TfrService::class.java)
        service.action = TfrService.ACTION_GET_TFR_LIST
        service.putExtra(TfrService.FORCE_REFRESH, force)
        requireActivity().startService(service)
    }

    @SuppressLint("SetTextI18n")
    private fun setEmptyText() {
        binding.empty.text = "No TFRs found"
    }

    private fun setListShown(show: Boolean) {
        if (show) {
            binding.empty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.empty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
    }

    private inner class TfrReceiver : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val tfrList = IntentCompat.getSerializableExtra(
                intent,
                TfrService.TFR_LIST,
                TfrList::class.java
            ) ?: return

            tfrList.entries.removeIf { it.isExpired }
            val count = tfrList.entries.size
            if (count > 0) {
                setActionBarSubtitle("$count TFRs found")
                setListShown(true)
            } else {
                setEmptyText()
                setActionBarSubtitle("")
                setListShown(false)
            }
            val adapter = TfrRecyclerViewAdapter(
                activityBase,
                tfrList.entries,
                ::onRecyclerItemClick)
            recyclerView.adapter = adapter
            isRefreshing = false
        }
    }
}