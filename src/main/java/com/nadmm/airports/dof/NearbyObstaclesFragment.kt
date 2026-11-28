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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nadmm.airports.LocationFragmentBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.databinding.DofTabsViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Obstacles screen: finds the obstacles near the location, and shows them
 * on its Map and List tabs. The location is followed here, for the whole
 * screen, so it keeps up whichever tab shows, and its prompts, such as for
 * location permission, show over both.
 */
class NearbyObstaclesFragment : LocationFragmentBase() {
    private var _binding: DofTabsViewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NearbyObstaclesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DofTabsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        _binding = null
        super.onDestroyView()
    }

    override fun startLocationTask() {
        val location = lastLocation ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val obstacles = withContext(Dispatchers.IO) {
                val db = getDatabase(DatabaseManager.DB_DOF)
                NearbyDofCursor(db, location, RADIUS_NM).use { c ->
                    val list = ArrayList<DofListDataModel>(c.count)
                    while (c.moveToNext()) {
                        list.add(DofListDataModel.fromCursor(c))
                    }
                    list
                }
            }
            viewModel.publish(NearbyObstacles(location, RADIUS_NM, obstacles))
        }
    }

    override fun showLocationMessage(message: String) {
        viewModel.publishMessage(message)
    }

    companion object {
        const val RADIUS_NM = 5

        // The map first: where the obstacles are matters most. The list,
        // tallest first, is a tab away.
        private val TABS = listOf(
            "Map" to NearbyObstaclesMapFragment::class.java,
            "List" to NearbyObstaclesListFragment::class.java,
        )
    }
}
