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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.databinding.TfrListViewBinding
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.CoroutineIntentService
import com.nadmm.airports.utils.FilterChips
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Lists the current TFRs, with chips on top to filter them by state, center
 * and type. The filters are remembered from one visit to the next.
 */
class TfrListFragment : FragmentBase() {

    private var _binding: TfrListViewBinding? = null
    private val binding get() = _binding!!
    val recyclerView get() = binding.recyclerView

    /** What a TFR can be filtered by, and where the choice is remembered. */
    private enum class Field(val title: String, val prefKey: String) {
        STATE("State", "tfr_filter_state"),
        CENTER("Center", "tfr_filter_center"),
        TYPE("Type", "tfr_filter_type");

        fun keyOf(tfr: Tfr): String = when (this) {
            STATE -> tfr.state
            CENTER -> tfr.facility
            TYPE -> tfr.type
        }.orEmpty()
    }

    // Every current TFR, before filtering
    private var allTfrs: List<Tfr> = emptyList()
    private var filterChips: FilterChips? = null
    private var filters: Map<Field, FilterChips.Filter> = emptyMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBarTitle("TFR List")
        setActionBarSubtitle("Loading...")
        setUpFilters()
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
                        allTfrs = tfrList.entries
                        showTfrs()
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

        _binding = TfrListViewBinding.inflate(inflater, container, false)

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

    private fun setUpFilters() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val bar = FilterChips(binding.tfrFilters, binding.tfrFilterChips, prefs) { showTfrs() }
        filters = mapOf(
            Field.STATE to binding.tfrFilterState,
            Field.CENTER to binding.tfrFilterCenter,
            Field.TYPE to binding.tfrFilterType,
        ).mapValues { (field, chip) ->
            bar.add(bar.Filter(chip, field.title, field.prefKey,
                label = { label(field, it) },
                allCount = { allTfrs.count { matches(it, except = field) } },
                choices = { choices(field) }))
        }
        filterChips = bar
        bar.update()
    }

    /** The values of [field] among the TFRs the other filters let through. */
    private fun choices(field: Field): List<FilterChips.Choice> {
        val counts = allTfrs.filter { matches(it, except = field) }
            .groupingBy { field.keyOf(it) }.eachCount()
        // Alphabetical, with no center or an unknown value last
        return counts.keys.sortedWith(compareBy(
            { label(field, it) in setOf(NO_VALUE_CENTER, NO_VALUE) },
            { label(field, it).lowercase() }))
            .map { FilterChips.Choice(it, label(field, it), counts[it]) }
    }

    private fun matches(tfr: Tfr, except: Field? = null): Boolean =
        filters.all { (field, filter) ->
            field == except || filter.value == null || field.keyOf(tfr) == filter.value
        }

    /** How a filter value reads on its chip and in its menu. */
    private fun label(field: Field, key: String): String = when {
        field == Field.CENTER && (key.isEmpty() || key == NO_CENTER) -> NO_VALUE_CENTER
        field == Field.CENTER ->
            allTfrs.firstOrNull { it.facility == key && !it.facilityName.isNullOrBlank() }
                ?.facilityName ?: key
        key.isEmpty() -> NO_VALUE
        else -> key
    }

    @SuppressLint("SetTextI18n")
    private fun showTfrs() {
        filterChips?.update()
        val shown = allTfrs.filter { matches(it) }
        val total = allTfrs.size
        binding.tfrFilters.visibility = if (total > 0) View.VISIBLE else View.GONE
        setActionBarSubtitle(when {
            total == 0 -> ""
            shown.size < total -> "${shown.size} of $total TFRs"
            else -> "$total TFRs found"
        })
        if (shown.isNotEmpty()) {
            setListShown(true)
        } else {
            binding.tfrListEmpty.text = if (total == 0) "No TFRs found" else "No TFRs match the filters"
            setListShown(false)
        }
        recyclerView.adapter = TfrRecyclerViewAdapter(activityBase, shown, ::onRecyclerItemClick)
    }

    private fun setListShown(show: Boolean) {
        if (show) {
            binding.tfrListEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.tfrListEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
    }

    companion object {
        // What the TFR list has for the facility of TFRs with no center, such
        // as those nationwide
        private const val NO_CENTER = "Not"
        // Labels for TFRs with no center, or no state or type
        private const val NO_VALUE_CENTER = "None"
        private const val NO_VALUE = "Unknown"
    }
}
