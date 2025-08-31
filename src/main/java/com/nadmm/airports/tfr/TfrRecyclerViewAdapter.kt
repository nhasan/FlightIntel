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
package com.nadmm.airports.tfr

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.databinding.TfrListItemBinding

class TfrRecyclerViewAdapter(
    val context: ActivityBase,
    val tfrList: List<TfrList.Tfr>,
    val onRecyclerItemClick: (TfrList.Tfr) -> Unit
) : RecyclerView.Adapter<TfrRecyclerViewAdapter.ViewHolder>()
{
    init {
        setHasStableIds(true)
    }

    inner class ViewHolder(val binding: TfrListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(tfr: TfrList.Tfr) {
            with(binding) {
                val index = tfr.notamId?.indexOf(' ') ?: -1
                if (index > 0) {
                    tfrAgency.text = tfr.notamId?.substring(0, index)
                } else {
                    tfrAgency.text = ""
                }
                if (tfr.notamId == tfr.name) {
                    tfrName.text = tfr.notamId
                } else {
                    tfrName.text = "${tfr.notamId} - ${tfr.name}"
                }
                tfrLocation.text = tfr.formatLocation()
                tfrTime.text = tfr.formatTimeRange(context)
                tfrActive.text = if (tfr.isExpired) "Expired" else if (tfr.isActive) "Active" else "Inactive"
                tfrType.text = tfr.type
                tfrAltitudes.text = tfr.formatAltitudeRange()

                root.setOnClickListener {
                    onRecyclerItemClick(tfr)
                }
            }
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TfrListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tfrList[position])
    }

    override fun getItemCount() = tfrList.size
}