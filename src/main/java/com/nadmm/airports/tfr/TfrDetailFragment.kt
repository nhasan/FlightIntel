/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.TimeUtils

class TfrDetailFragment : FragmentBase() {
    private var mTfr: Tfr? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tfr_detail_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        mTfr = args!!.getSerializable(TfrListActivity.EXTRA_TFR) as Tfr?
        val btnGraphic = view.findViewById<Button>(R.id.btnViewGraphic)
        if (mTfr!!.notamId != null && mTfr!!.notamId!!.isNotEmpty()) {
            btnGraphic.setOnClickListener {
                val intent = Intent(activity, TfrImageActivity::class.java)
                intent.putExtra(TfrListActivity.EXTRA_TFR, mTfr)
                startActivity(intent)
            }
        } else {
            btnGraphic.visibility = View.GONE
        }
        var layout = view.findViewById<LinearLayout>(R.id.tfr_header_layout)
        addRow(layout!!, "NOTAM Id", mTfr!!.notamId)
        if (mTfr!!.type != null && mTfr!!.type!!.isNotEmpty()) {
            addRow(layout, "Type", mTfr!!.type)
        }
        addRow(
            layout,
            "Status",
            if (mTfr!!.isExpired) "Expired" else if (mTfr!!.isActive) "Active" else "Inactive"
        )
        addRow(layout, "Location", mTfr!!.formatLocation())
        if (mTfr!!.facilityType!!.isNotEmpty() && mTfr!!.facility!!.isNotEmpty()) {
            addRow(layout, mTfr!!.facilityType!!, mTfr!!.facility)
        }
        addRow(layout, "Time", mTfr!!.formatTimeRange(activityBase))
        addRow(layout, "Altitude", mTfr!!.formatAltitudeRange())
        layout = view.findViewById(R.id.tfr_time_layout)
        if (mTfr!!.createTime < Long.MAX_VALUE) {
            addRow(
                layout, "Created",
                TimeUtils.formatDateTimeYear(activityBase, mTfr!!.createTime)
            )
        }
        if (mTfr!!.modifyTime < Long.MAX_VALUE && mTfr!!.modifyTime > mTfr!!.createTime) {
            addRow(
                layout, "Modified",
                TimeUtils.formatDateTimeYear(activityBase, mTfr!!.modifyTime)
            )
        }
        layout = view.findViewById(R.id.tfr_text_layout)
        addRow(layout, mTfr!!.text!!.replace("\\n", "\n"))
        val tv = view.findViewById<TextView>(R.id.tfr_warning_text)
        tv.text = ("Depicted TFR data may not be a complete listing. Pilots should not use "
                + "the information for flight planning purposes. For the latest information, "
                + "call your local Flight Service Station at 1-800-WX-BRIEF.")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarTitle("TFR Details")
        setActionBarSubtitle(mTfr!!.name!!)
        setFragmentContentShown(true)
    }
}