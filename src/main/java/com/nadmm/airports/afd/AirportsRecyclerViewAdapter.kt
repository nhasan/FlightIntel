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
package com.nadmm.airports.afd

import android.annotation.SuppressLint
import android.database.Cursor
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.databinding.AirportListItemBinding

class AirportsRecyclerViewAdapter(
    val cursor: Cursor,
    private val onRecyclerItemClick: (AirportListDataModel) -> Unit
): RecyclerView.Adapter<AirportsRecyclerViewAdapter.ViewHolder>()
{
    private val idColumnIndex: Int

    init {
        setHasStableIds(true)
        idColumnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
    }

    override fun getItemId(position: Int): Long {
        cursor.moveToPosition(position)
        return cursor.getLong(idColumnIndex)
    }

    inner class ViewHolder(val binding: AirportListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(model: AirportListDataModel) {
            with(binding) {
                facilityId.text = model.facilityId
                facilityName.text = model.facilityName
                location.text = model.location
                otherInfo.text = model.otherInfo
                if (model.position != null) {
                    distance.visibility = View.VISIBLE
                    distance.text = model.position
                } else {
                    distance.visibility = View.GONE
                }
                root.setOnClickListener {
                    onRecyclerItemClick(model)
                }
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = AirportListItemBinding.inflate(
            LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        cursor.moveToPosition(position)
        val model = AirportListDataModel.fromCursor(cursor)
        holder.bind(model)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cursor.close()
    }

    override fun getItemCount(): Int {
        return cursor.count
    }
}