/*
 * FlightIntel for Pilots
 *
 * Copyright 2018-2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.dof

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nadmm.airports.LocationFragmentBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.databinding.DofTabsViewBinding
import com.nadmm.airports.utils.FilterChips
import com.nadmm.airports.utils.FilterChips.Choice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * The Obstacles screen: finds the obstacles near the location, and shows them
 * on its Map and List tabs. The location is followed here, for the whole
 * screen, so it keeps up whichever tab shows, and its prompts, such as for
 * location permission, show over both.
 *
 * Chips above the tabs filter by height and lighting, and set the search
 * radius; the choices are remembered from one visit to the next.
 */
class NearbyObstaclesFragment : LocationFragmentBase() {
    private var _binding: DofTabsViewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NearbyObstaclesViewModel by activityViewModels()

    // The obstacles within the radius searched, before the other filters
    private var found: List<DofListDataModel> = emptyList()
    private var foundAt: Location? = null
    private var foundRadius = 0

    private var filterChips: FilterChips? = null
    private lateinit var height: FilterChips.Filter
    private lateinit var lighting: FilterChips.Filter
    private lateinit var radius: FilterChips.Filter

    private val radiusNm: Int
        get() = radius.value?.toIntOrNull() ?: DEFAULT_RADIUS_NM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DofTabsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpFilters()
        setActionBarSubtitle("Within $radiusNm NM radius")
        binding.dofPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = TABS.size

            override fun createFragment(position: Int): Fragment =
                TABS[position].second.getDeclaredConstructor().newInstance().apply {
                    // The map labels the place passed in, such as an airport
                    arguments = this@NearbyObstaclesFragment.arguments
                }
        }
        TabLayoutMediator(binding.dofTabs, binding.dofPager) { tab, position ->
            tab.text = TABS[position].first
        }.attach()
    }

    override fun onDestroyView() {
        filterChips = null
        _binding = null
        super.onDestroyView()
    }

    private fun setUpFilters() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val bar = FilterChips(binding.dofFilters, binding.dofFilterChips, prefs) { onFiltersChanged() }
        height = bar.add(bar.Filter(binding.dofFilterHeight, "Height", "dof_filter_height",
            label = ::heightLabel,
            allCount = { countExcept(height) },
            choices = {
                HEIGHTS_FT.map { ft ->
                    Choice(ft.toString(), heightLabel(ft.toString()),
                        found.count { matches(it, except = height) && it.aglHeight >= ft })
                }
            }))
        lighting = bar.add(bar.Filter(binding.dofFilterLighting, "Lighting", "dof_filter_lighting",
            label = { if (it == LIGHTED) "Lighted" else "Unlit" },
            allCount = { countExcept(lighting) },
            choices = {
                val others = found.filter { matches(it, except = lighting) }
                listOf(
                    Choice(LIGHTED, "Lighted", others.count { it.isLighted }),
                    // Unknown lighting counts as unlit: it can't be relied on
                    Choice(UNLIT, "Unlit", others.count { !it.isLighted }),
                )
            }))
        radius = bar.add(bar.Filter(binding.dofFilterRadius, "Radius", "dof_filter_radius",
            default = DEFAULT_RADIUS_NM.toString(),
            label = { "$it NM" },
            choices = { RADII_NM.map { Choice(it.toString(), "$it NM") } }))
        filterChips = bar
        bar.update()
    }

    private fun heightLabel(key: String): String =
        String.format(Locale.US, "%,d ft+", key.toIntOrNull() ?: 0)

    /** Whether the filters other than [except] let [obstacle] through. */
    private fun matches(obstacle: DofListDataModel, except: FilterChips.Filter? = null): Boolean {
        if (except !== height) {
            height.value?.toIntOrNull()?.let { if (obstacle.aglHeight < it) return false }
        }
        if (except !== lighting) {
            when (lighting.value) {
                LIGHTED -> if (!obstacle.isLighted) return false
                UNLIT -> if (obstacle.isLighted) return false
            }
        }
        return true
    }

    private fun countExcept(filter: FilterChips.Filter) = found.count { matches(it, except = filter) }

    private fun onFiltersChanged() {
        // A new radius needs a new search; the others only filter what was found
        if (radiusNm != foundRadius) startLocationTask() else publish()
    }

    override fun startLocationTask() {
        val location = lastLocation ?: return
        val radiusNm = radiusNm
        viewLifecycleOwner.lifecycleScope.launch {
            val obstacles = withContext(Dispatchers.IO) {
                val db = getDatabase(DatabaseManager.DB_DOF)
                NearbyDofCursor(db, location, radiusNm).use { c ->
                    val list = ArrayList<DofListDataModel>(c.count)
                    while (c.moveToNext()) {
                        list.add(DofListDataModel.fromCursor(c))
                    }
                    list
                }
            }
            found = obstacles
            foundAt = location
            foundRadius = radiusNm
            publish()
        }
    }

    /** Shows what the filters let through, on both tabs. */
    private fun publish() {
        filterChips?.update()
        val location = foundAt ?: return
        val shown = found.filter { matches(it) }
        viewModel.publish(NearbyObstacles(location, foundRadius, shown, found.size))
        setActionBarSubtitle(
            if (shown.size < found.size) "${shown.size} of ${found.size} within $foundRadius NM"
            else "Within $foundRadius NM radius")
    }

    override fun showLocationMessage(message: String) {
        viewModel.publishMessage(message)
    }

    companion object {
        const val DEFAULT_RADIUS_NM = 5
        private val RADII_NM = listOf(5, 10, 20)
        // Above ground: most obstacles are short, and hiding them shows those
        // that matter at pattern and cruise altitudes. 50 ft drops the fences,
        // signs and short poles that crowd the map further out.
        private val HEIGHTS_FT = listOf(50, 200, 500)
        private const val LIGHTED = "lighted"
        private const val UNLIT = "unlit"

        // The map first: where the obstacles are matters most. The list,
        // tallest first, is a tab away.
        private val TABS = listOf(
            "Map" to NearbyObstaclesMapFragment::class.java,
            "List" to NearbyObstaclesListFragment::class.java,
        )
    }
}
