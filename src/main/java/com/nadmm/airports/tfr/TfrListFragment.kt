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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.databinding.TfrListViewBinding
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.CoroutineIntentService
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
    private enum class Filter(val title: String, val prefKey: String) {
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
    // The chosen value of each filter, or none for all
    private val chosen = mutableMapOf<Filter, String>()
    private val chips = mutableMapOf<Filter, Chip>()
    // The chips' own close icon, shown once a value is chosen
    private var clearIcon: Drawable? = null

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
        chips[Filter.STATE] = binding.tfrFilterState
        chips[Filter.CENTER] = binding.tfrFilterCenter
        chips[Filter.TYPE] = binding.tfrFilterType
        clearIcon = binding.tfrFilterState.closeIcon

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        for ((filter, chip) in chips) {
            prefs.getString(filter.prefKey, null)?.let { chosen[filter] = it }
            // A tap offers the values to pick from
            chip.setOnClickListener { showChoices(filter) }
            chip.setOnCloseIconClickListener {
                if (filter in chosen) choose(filter, null) else showChoices(filter)
            }
        }
        updateChips()
        // Again whenever the row's width changes, such as on rotation
        binding.tfrFilters.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) binding.tfrFilters.post { fitChips() }
        }
    }

    /**
     * Shares the row's width among the chips, so all of them, and each one's
     * icon, stay on screen. Chips keep their own width while they all fit;
     * otherwise the widest are shortened, their labels ending in an ellipsis,
     * to equal shares of the width the narrower ones leave.
     */
    private fun fitChips() {
        val row = _binding?.tfrFilters ?: return
        val spacing = binding.tfrFilterChips.chipSpacingHorizontal * (chips.size - 1)
        var remaining = row.width - row.paddingStart - row.paddingEnd - spacing
        if (remaining <= 0) return
        val natural = chips.values.associateWith { chip ->
            chip.maxWidth = Int.MAX_VALUE
            chip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            chip.measuredWidth
        }
        var left = natural.size
        for ((chip, width) in natural.entries.sortedBy { it.value }) {
            val fitted = minOf(width, remaining / left)
            chip.maxWidth = fitted
            remaining -= fitted
            left--
        }
    }

    /** Offers the values of [filter] among the TFRs the other filters let through. */
    private fun showChoices(filter: Filter) {
        val chip = chips[filter] ?: return
        // Tapping a checkable chip toggles it; it should show only the choice
        chip.isChecked = filter in chosen

        val others = allTfrs.filter { matches(it, except = filter) }
        val counts = others.groupingBy { filter.keyOf(it) }.eachCount().toMutableMap()
        // A remembered choice stays on offer even when no TFR has it now
        chosen[filter]?.let { counts.putIfAbsent(it, 0) }
        // Alphabetical, with no center or an unknown value last
        val keys = counts.keys.sortedWith(compareBy(
            { label(filter, it) in setOf(NO_VALUE_CENTER, NO_VALUE) },
            { label(filter, it).lowercase() }))

        PopupMenu(requireContext(), chip).apply {
            menu.add(0, 0, 0, "All (${others.size})")
            keys.forEachIndexed { i, key ->
                menu.add(0, i + 1, i + 1, "${label(filter, key)} (${counts[key]})")
            }
            setOnMenuItemClickListener { item ->
                choose(filter, if (item.itemId == 0) null else keys[item.itemId - 1])
                true
            }
            show()
        }
    }

    private fun choose(filter: Filter, key: String?) {
        if (key == null) chosen.remove(filter) else chosen[filter] = key
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            if (key == null) remove(filter.prefKey) else putString(filter.prefKey, key)
        }
        showTfrs()
    }

    private fun matches(tfr: Tfr, except: Filter? = null): Boolean =
        chosen.all { (filter, key) -> filter == except || filter.keyOf(tfr) == key }

    /** How a filter value reads on its chip and in its menu. */
    private fun label(filter: Filter, key: String): String = when {
        filter == Filter.CENTER && (key.isEmpty() || key == NO_CENTER) -> NO_VALUE_CENTER
        filter == Filter.CENTER ->
            allTfrs.firstOrNull { it.facility == key && !it.facilityName.isNullOrBlank() }
                ?.facilityName ?: key
        key.isEmpty() -> NO_VALUE
        else -> key
    }

    private fun updateChips() {
        val arrow = ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_arrow_drop_down_24)
        for ((filter, chip) in chips) {
            val key = chosen[filter]
            chip.text = key?.let { label(filter, it) } ?: filter.title
            chip.isChecked = key != null
            chip.closeIcon = if (key != null) clearIcon else arrow
            chip.closeIconContentDescription =
                if (key != null) "Clear ${filter.title.lowercase()} filter"
                else "Choose ${filter.title.lowercase()}"
        }
        // Once the new labels are measured
        _binding?.tfrFilters?.post { fitChips() }
    }

    @SuppressLint("SetTextI18n")
    private fun showTfrs() {
        updateChips()
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
