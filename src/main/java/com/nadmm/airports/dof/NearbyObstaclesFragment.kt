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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nadmm.airports.LocationFragmentBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.databinding.DofTabsViewBinding
import com.nadmm.airports.utils.FilterChips
import com.nadmm.airports.utils.FilterChips.Choice
import com.nadmm.airports.utils.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

/**
 * The Obstacles screen: shows the tallest obstacles in the map's view, up to
 * [MAX_SHOWN], on its Map and List tabs. Zooming out shows only the taller
 * ones, and zooming in the shorter ones too. The map opens on the location,
 * which is followed here, for the whole screen, so the distances keep up
 * whichever tab shows, and its prompts, such as for location permission,
 * show over both.
 *
 * Chips above the tabs filter by height, lighting and type; the choices are
 * remembered from one visit to the next.
 */
class NearbyObstaclesFragment : LocationFragmentBase() {
    private var _binding: DofTabsViewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NearbyObstaclesViewModel by activityViewModels()

    // The obstacles of an area around the view, and the one being loaded
    private var area: ObstacleArea? = null
    private var loading: Job? = null
    private var loadingBox: BoundingBox? = null

    private var filterChips: FilterChips? = null
    private lateinit var height: FilterChips.Filter
    private lateinit var lighting: FilterChips.Filter
    private lateinit var type: FilterChips.Filter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DofTabsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpFilters()
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

        // Each time the map settles on a new view
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewport.collect { update() }
            }
        }
    }

    override fun onDestroyView() {
        filterChips = null
        loading = null
        loadingBox = null
        _binding = null
        super.onDestroyView()
    }

    private fun setUpFilters() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val bar = FilterChips(binding.dofFilters, binding.dofFilterChips, prefs) { update() }
        height = bar.add(bar.Filter(binding.dofFilterHeight, "Height", "dof_filter_height",
            label = ::heightLabel,
            allCount = { inView().count { matches(it, except = height) } },
            choices = {
                val others = inView().filter { matches(it, except = height) }
                HEIGHTS_FT.map { ft ->
                    Choice(ft.toString(), heightLabel(ft.toString()), others.count { it.aglHeight >= ft })
                }
            }))
        lighting = bar.add(bar.Filter(binding.dofFilterLighting, "Lighting", "dof_filter_lighting",
            label = { if (it == LIGHTED) "Lighted" else "Unlit" },
            allCount = { inView().count { matches(it, except = lighting) } },
            choices = {
                val others = inView().filter { matches(it, except = lighting) }
                listOf(
                    Choice(LIGHTED, "Lighted", others.count { it.isLighted }),
                    // Unknown lighting counts as unlit: it can't be relied on
                    Choice(UNLIT, "Unlit", others.count { !it.isLighted }),
                )
            }))
        type = bar.add(bar.Filter(binding.dofFilterType, "Type", "dof_filter_type",
            allCount = { inView().count { matches(it, except = type) } },
            choices = {
                inView().filter { matches(it, except = type) }
                    .groupingBy { it.typeName }.eachCount()
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                    .map { (name, count) -> Choice(name, name, count) }
            }))
        filterChips = bar
        bar.update()
    }

    private fun heightLabel(key: String): String =
        String.format(Locale.US, "%,d ft+", key.toIntOrNull() ?: 0)

    /** Whether the filters other than [except] let [row] through. */
    private fun matches(row: ObstacleRow, except: FilterChips.Filter? = null): Boolean {
        if (except !== height) {
            height.value?.toIntOrNull()?.let { if (row.aglHeight < it) return false }
        }
        if (except !== lighting) {
            when (lighting.value) {
                LIGHTED -> if (!row.isLighted) return false
                UNLIT -> if (row.isLighted) return false
            }
        }
        if (except !== type) {
            type.value?.let { if (row.typeName != it) return false }
        }
        return true
    }

    /** The map's view, or until it gives one, the view it opens on. */
    private fun view(reference: Location): BoundingBox =
        viewModel.viewport.value ?: startBox(reference)

    /** The obstacles in view, tallest first, before the filters. */
    private fun inView(): List<ObstacleRow> {
        val reference = lastLocation ?: return emptyList()
        val area = area ?: return emptyList()
        val view = view(reference)
        return area.rows.filter { isInView(it, view) }
    }

    private fun isInView(row: ObstacleRow, view: BoundingBox): Boolean =
        row.latitude in view.latSouth..view.latNorth && row.longitude in view.lonWest..view.lonEast

    override fun startLocationTask() {
        update()
    }

    /**
     * Shows the tallest obstacles in the view, loading the area around it
     * first when the view has moved out of the one loaded.
     */
    private fun update() {
        if (_binding == null) return
        val reference = lastLocation ?: return
        val view = view(reference)
        val area = area
        if (area != null && area.covers(view)) {
            publish(area, view, reference)
        } else if (loadingBox?.let { box -> loading?.isActive == true && ObstacleArea.covers(box, view) } != true) {
            load(view)
        }
    }

    private fun load(view: BoundingBox) {
        // The view and as much again on each side, so moving about needs no load
        val box = view.increaseByScale(AREA_SCALE)
        loading?.cancel()
        loadingBox = box
        loading = viewLifecycleOwner.lifecycleScope.launch {
            area = withContext(Dispatchers.IO) {
                ObstacleArea.load(getDatabase(DatabaseManager.DB_DOF), box)
            }
            update()
        }
    }

    /** Shows the tallest in [view] that the filters let through, on both tabs. */
    private fun publish(area: ObstacleArea, view: BoundingBox, reference: Location) {
        filterChips?.update()
        val declination = GeoUtils.getMagneticDeclination(reference)
        var inView = 0
        var matching = 0
        val shown = ArrayList<DofListDataModel>()
        // Tallest first, so the first that match are the tallest
        for (row in area.rows) {
            if (!isInView(row, view)) continue
            inView++
            if (!matches(row)) continue
            matching++
            if (shown.size < MAX_SHOWN) shown.add(DofListDataModel.fromRow(row, reference, declination))
        }
        viewModel.publish(NearbyObstacles(reference, shown, inView, matching))
        setActionBarSubtitle(when {
            matching > shown.size -> "Tallest ${count(shown.size)} of ${count(matching)} in view"
            matching < inView -> "${count(matching)} of ${count(inView)} in view"
            else -> "${count(inView)} in view"
        })
    }

    private fun count(n: Int) = String.format(Locale.US, "%,d", n)

    override fun showLocationMessage(message: String) {
        viewModel.publishMessage(message)
    }

    companion object {
        /** At most this many obstacles show, the tallest in view. */
        const val MAX_SHOWN = 100
        /** The map opens to show this far around the location. */
        const val START_RADIUS_NM = 5
        // How much bigger than the view the area loaded around it is
        private const val AREA_SCALE = 3f

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

        /** The view the map opens on: [START_RADIUS_NM] around [location]. */
        fun startBox(location: Location): BoundingBox {
            val center = GeoPoint(location.latitude, location.longitude)
            val meters = START_RADIUS_NM * GeoUtils.METERS_PER_NAUTICAL_MILE.toDouble()
            return BoundingBox.fromGeoPointsSafe(Polygon.pointsAsCircle(center, meters))
        }
    }
}
