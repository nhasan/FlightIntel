/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2026 Nadeem Hasan <nhasan@nadmm.com>
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.tfr.TfrList.TfrArea
import com.nadmm.airports.tfr.TfrList.TfrVertex
import com.nadmm.airports.tfr.TfrList.TfrVertexType
import com.nadmm.airports.utils.TimeUtils
import kotlin.math.abs

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
        val tfr = mTfr!!
        var layout = view.findViewById<LinearLayout>(R.id.tfr_header_layout)
        addRow(layout!!, "NOTAM Id", tfr.notamId)
        if (!tfr.type.isNullOrEmpty()) {
            addRow(layout, "Type", tfr.type)
        }
        if (!tfr.reason.isNullOrBlank()) {
            addRow(layout, "Reason", null, tfr.reason)
        }
        addRow(
            layout,
            "Status",
            if (tfr.isExpired) "Expired" else if (tfr.isActive) "Active" else "Inactive"
        )
        addRow(layout, "Location", null, tfr.formatLocation())
        addValueRow(layout, "Time", tfr.formatTimeRange(activityBase))
        addValueRow(layout, "Altitude", tfr.formatAltitudeRange())
        if (!tfr.replacedNotamId.isNullOrEmpty()) {
            addRow(layout, "Replaces", tfr.replacedNotamId, tfr.replacedReason)
        }
        if (!tfr.facilityType.isNullOrEmpty() && !tfr.facility.isNullOrEmpty()) {
            addRow(layout, tfr.facilityType!!, tfr.facility, tfr.facilityName)
        }
        if (!tfr.authority.isNullOrBlank()) {
            addRow(layout, "Authority", null, tfr.authority)
        }

        val sections = view.findViewById<LinearLayout>(R.id.tfr_sections_layout)
        tfr.areas.forEachIndexed { index, area ->
            val name = area.name
                ?: if (tfr.areas.size > 1) "Area ${index + 1}" else "Affected Area"
            addAreaRows(addSection(sections, name), area)
        }
        if (tfr.restriction != null || tfr.instructions.isNotEmpty()) {
            val card = addSection(sections, "Operating Restrictions")
            tfr.restriction?.let { addRow(card, it) }
            tfr.instructions.forEach { addRow(card, it) }
        }

        layout = view.findViewById(R.id.tfr_text_layout)
        addRow(layout, tfr.text!!.replace("\\n", "\n"))
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
        val tv = view.findViewById<TextView>(R.id.tfr_warning_text)
        tv.text = ("Depicted TFR data may not be a complete listing. Pilots should not use "
                + "the information for flight planning purposes. For the latest information, "
                + "call your local Flight Service Station at 1-800-WX-BRIEF.")

        setFragmentContentShown(true)
    }

    override fun onResume() {
        super.onResume()
        // The Map tab shows the location instead; show the name again here
        mTfr?.name?.let { setActionBarSubtitle(it) }
    }

    /**
     * A row with its value beside the label, or under it when too long to
     * fit beside it.
     */
    private fun addValueRow(layout: LinearLayout, label: String, value: String) {
        if (value.length > SHORT_VALUE_LENGTH) addRow(layout, label, null, value)
        else addRow(layout, label, value)
    }

    /** Adds a labeled card to [parent] and returns the card for its rows. */
    private fun addSection(parent: LinearLayout, title: String): LinearLayout {
        val label = inflate<TextView>(R.layout.tfr_detail_label)
        label.text = title
        parent.addView(label)
        val card = inflate<LinearLayout>(R.layout.tfr_detail_card)
        parent.addView(card)
        return card
    }

    /** The definition of an area, then its altitudes and effective times. */
    private fun addAreaRows(card: LinearLayout, area: TfrArea) {
        for (part in area.parts) {
            if (part.excluded) addRow(card, "Excluding")
            addPartRows(card, part.vertices)
        }
        TfrAreaFormat.formatAltitudeRange(area.minAltitude, area.maxAltitude)?.let {
            addValueRow(card, "Altitude", it)
        }
        val activity = activityBase
        area.activeTime?.let { addRow(card, "From", TimeUtils.formatDateTimeYear(activity, it)) }
        area.expireTime?.let { addRow(card, "To", TimeUtils.formatDateTimeYear(activity, it)) }
        TfrMapLabels.dailyWindow(area.dailyStart, area.dailyEnd)?.let { (start, end) ->
            addRow(card, "Daily", TfrAreaFormat.formatTimeOfDayRange(activity, start, end))
        }
    }

    /**
     * A circle as its center and radius, or a boundary as its points in order,
     * with any arcs between them, closing on the point it started from.
     */
    private fun addPartRows(card: LinearLayout, vertices: List<TfrVertex>) {
        var first = true
        for (vertex in vertices) {
            val position = TfrAreaFormat.formatCoordinates(vertex.latitude, vertex.longitude)
            val fix = vertex.fix?.let(TfrAreaFormat::formatFix)
            when (vertex.type) {
                TfrVertexType.CIRCLE -> {
                    addRow(card, "Center", position, fix)
                    vertex.radius?.let { addRow(card, "Radius", TfrAreaFormat.formatNm(it)) }
                }
                TfrVertexType.POINT -> {
                    addRow(card, if (first) "From" else "To", position, fix)
                    first = false
                }
                TfrVertexType.ARC_CW, TfrVertexType.ARC_CCW -> {
                    val label = if (vertex.type == TfrVertexType.ARC_CW) "Clockwise arc"
                        else "Counter-clockwise arc"
                    val center = listOfNotNull("Centered on $position", fix).joinToString("\n")
                    addRow(card, label, vertex.radius?.let { TfrAreaFormat.formatNm(it) + " radius" },
                        center)
                }
            }
        }
        // The FAA often repeats the first point at the end; add it when not
        val origin = vertices.firstOrNull()?.takeIf { it.type == TfrVertexType.POINT }
        val last = vertices.lastOrNull()
        if (origin != null && last != null && vertices.size > 1
            && (last.type != TfrVertexType.POINT
                || abs(last.latitude - origin.latitude) > 1e-4
                || abs(last.longitude - origin.longitude) > 1e-4)
        ) {
            addRow(card, "To", TfrAreaFormat.formatCoordinates(origin.latitude, origin.longitude),
                origin.fix?.let(TfrAreaFormat::formatFix))
        }
    }

    companion object {
        // Longer values squeeze their label when beside it
        private const val SHORT_VALUE_LENGTH = 24
    }
}
