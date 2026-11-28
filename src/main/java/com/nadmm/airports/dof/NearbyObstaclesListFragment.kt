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
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.R
import com.nadmm.airports.RecyclerViewFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The List tab of the Obstacles screen: the same obstacles as the Map tab,
 * those in its view, tallest first, or why there are none yet.
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
                        // Says where they're from, as panning the map changes them
                        recyclerView.adapter = ConcatAdapter(
                            HeaderAdapter(HEADER_TEXT), DofRecyclerViewAdapter(obstacles))
                        setEmptyText(when {
                            message != null -> message
                            nearby == null -> WAITING_TEXT
                            nearby.inView > 0 -> "No obstacles match the filters"
                            else -> "No obstacles in the map's view"
                        })
                        setListShown(obstacles.isNotEmpty())
                    }
            }
        }
    }

    /** One line of text above the list. */
    private class HeaderAdapter(private val text: String) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val padding = parent.resources.getDimensionPixelSize(R.dimen.item_padding)
            val view = TextView(parent.context).apply {
                TextViewCompat.setTextAppearance(this, R.style.TextSmall)
                setPadding(padding, 0, padding, padding / 2)
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as TextView).text = text
        }

        override fun getItemCount() = 1
    }

    companion object {
        private const val WAITING_TEXT =
            "Obstacles appear here once your location is known."
        private const val HEADER_TEXT = "The obstacles in the map's view, tallest first"
    }
}
