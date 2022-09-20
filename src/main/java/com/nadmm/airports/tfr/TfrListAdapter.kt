/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2022 Nadeem Hasan <nhasan@nadmm.com>
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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import java.util.*

class TfrListAdapter(private val mContext: ActivityBase, tfrList: TfrList) : BaseAdapter() {
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val mTfrList: TfrList = tfrList
    override fun getCount(): Int {
        return mTfrList.entries.size
    }

    override fun getItem(position: Int): Any {
        return mTfrList.entries[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view  = convertView ?: mInflater.inflate(R.layout.tfr_list_item, null)
        val tfr = getItem(position) as Tfr
        var tv: TextView
        val index = tfr.notamId!!.indexOf(' ')
        if (index > 0) {
            tv = view.findViewById(R.id.tfr_agency)
            tv.text = tfr.notamId!!.substring(0, index)
        }
        tv = view.findViewById(R.id.tfr_name)
        if (tfr.notamId == tfr.name) {
            tv.text = tfr.notamId
        } else {
            tv.text = String.format(Locale.US, "%s - %s", tfr.notamId, tfr.name)
        }
        tv = view.findViewById(R.id.tfr_location)
        tv.text = tfr.formatLocation()
        tv = view.findViewById(R.id.tfr_time)
        tv.text = tfr.formatTimeRange(mContext)
        tv = view.findViewById(R.id.tfr_active)
        tv.text = if (tfr.isExpired) "Expired" else if (tfr.isActive) "Active" else "Inactive"
        tv = view.findViewById(R.id.tfr_type)
        tv.text = tfr.type
        tv = view.findViewById(R.id.tfr_altitudes)
        tv.text = tfr.formatAltitudeRange()
        return view
    }
}