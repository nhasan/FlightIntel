/*
 * FlightIntel for Pilots
 *
 * Copyright 2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.afd.AirportsRecyclerAdapter
import com.nadmm.airports.databinding.RecyclerViewLayoutBinding
import com.nadmm.airports.utils.UiUtils

abstract class RecyclerViewFragment: FragmentBase() {

    private var _binding: RecyclerViewLayoutBinding? = null
    private val binding get() = _binding!!
    val recyclerView get() = binding.recyclerView

    val adapter get() = recyclerView.adapter as? AirportsRecyclerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = RecyclerViewLayoutBinding.inflate(inflater, container, false)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        UiUtils.setupWindowInsetsListener(recyclerView)

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.adapter = null
    }

    protected open fun setCursor(cursor: Cursor) {
        recyclerView.adapter = newListAdapter(cursor)
        setListShown(recyclerView.adapter != null && recyclerView.adapter!!.itemCount > 0)
    }

    protected open fun newListAdapter(cursor: Cursor?): RecyclerView.Adapter<*>? {
        return null
    }

    protected fun setEmptyText(text: String) {
        binding.empty.text = text
    }

    protected fun setListShown(show: Boolean) {
        if (show) {
            binding.empty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.empty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
    }
}