/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nadmm.airports.RecyclerViewFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The List tab of the Obstacles screen: the obstacles the screen found,
 * tallest first, or why there are none yet.
 */
class NearbyObstaclesListFragment : RecyclerViewFragment() {
    private val viewModel: NearbyObstaclesViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nearby.combine(viewModel.message) { nearby, message -> nearby to message }
                    .collect { (nearby, message) ->
                        val obstacles = nearby?.obstacles.orEmpty()
                        recyclerView.adapter = DofRecyclerViewAdapter(obstacles)
                        setEmptyText(when {
                            message != null -> message
                            nearby == null -> WAITING_TEXT
                            nearby.total > 0 -> "No obstacles match the filters"
                            else -> "No obstacles within ${nearby.radiusNm} NM"
                        })
                        setListShown(obstacles.isNotEmpty())
                    }
            }
        }
    }

    companion object {
        private const val WAITING_TEXT =
            "Obstacles appear here once your location is known."
    }
}
