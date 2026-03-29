/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2026 Nadeem Hasan <nhasan@nadmm.com>
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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.databinding.RecyclerViewLayoutBinding
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.CoroutineIntentService
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class TfrListFragment : FragmentBase() {

    private var _binding: RecyclerViewLayoutBinding? = null
    private val binding get() = _binding!!
    val recyclerView get() = binding.recyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBarTitle("TFR List")
        setActionBarSubtitle("Loading...")
        requestTfrList(false)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CoroutineIntentService.Events.events.filterIsInstance<Bundle>().collect { bundle ->
                    val action = bundle.getString("ACTION")
                    if (action == TfrService.ACTION_GET_TFR_LIST) {
                        val tfrList = BundleCompat.getSerializable(
                            bundle,
                            TfrService.TFR_LIST,
                            TfrList::class.java
                        ) ?: return@collect

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
                            ::onRecyclerItemClick
                        )
                        recyclerView.adapter = adapter
                        isRefreshing = false
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.adapter = null
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
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
}
