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
package com.nadmm.airports.afd

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.UiUtils.setRunwayDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.roundToInt

class RunwaysFragment : FragmentBase() {
    private var mDeclination = 0f
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.runway_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val args = arguments
        args?.let {
            val siteNumber = args.getString(Airports.SITE_NUMBER)
            val runwayId = args.getString(Runways.RUNWAY_ID)
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(siteNumber!!, runwayId!!)
                }
                showDetails(result)
            }
        }
    }

    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        showAirportTitle(apt)
        val rwy = result[1]
        val runwayId = rwy!!.getString(rwy.getColumnIndexOrThrow(Runways.RUNWAY_ID))
        val isHelipad = runwayId.startsWith("H")
        if (isHelipad) {
            showHelipadInformation(result)
        } else {
            showRunwayInformation(result)
            showRunwayEndInformation(result, true)
            showRunwayEndInformation(result, false)
        }
        showCommonRemarks(result, runwayId)
        setFragmentContentShown(true)
    }

    @SuppressLint("SetTextI18n")
    private fun showRunwayInformation(result: Array<Cursor?>) {
        val layout = findViewById<LinearLayout>(R.id.rwy_common_details) ?: return
        val tv = findViewById<TextView>(R.id.rwy_common_label) ?: return
        val rwy = result[1]
        val runwayId = rwy!!.getString(rwy.getColumnIndexOrThrow(Runways.RUNWAY_ID))
        val length = rwy.getInt(rwy.getColumnIndexOrThrow(Runways.RUNWAY_LENGTH))
        val width = rwy.getInt(rwy.getColumnIndexOrThrow(Runways.RUNWAY_WIDTH))
        var heading = rwy.getInt(rwy.getColumnIndexOrThrow(Runways.BASE_END_HEADING))
        heading = if (heading > 0) {
            DataUtils.calculateMagneticHeading(heading, mDeclination.roundToInt())
        } else {
            // Actual heading is not available, try to deduce it from runway id
            DataUtils.getRunwayHeading(runwayId)
        }
        tv.text = "Runway $runwayId"
        setRunwayDrawable(requireActivity(), tv, runwayId, length, heading)
        addRow(layout, "Dimensions", FormatUtils.formatRunway(width, length))
        val surfaceType = rwy.getString(rwy.getColumnIndexOrThrow(Runways.SURFACE_TYPE))
        addRow(layout, "Surface type", DataUtils.decodeSurfaceType(surfaceType))
        val surfaceTreat = rwy.getString(rwy.getColumnIndexOrThrow(Runways.SURFACE_TREATMENT))
        addRow(layout, "Surface treatment", DataUtils.decodeSurfaceTreatment(surfaceTreat))
        val edgeLights = rwy.getString(rwy.getColumnIndexOrThrow(Runways.EDGE_LIGHTS_INTENSITY))
        addRow(layout, "Edge lights", DataUtils.decodeRunwayEdgeLights(edgeLights))
    }

    @SuppressLint("SetTextI18n")
    private fun showRunwayEndInformation(result: Array<Cursor?>, base: Boolean) {
        val apt = result[0] ?: return
        val rwy = result[1] ?: return
        val sb = StringBuilder()
        val icaoCode = apt.getString(apt.getColumnIndexOrThrow(Airports.ICAO_CODE))
        val endId = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_ID else Runways.RECIPROCAL_END_ID))
        var heading = rwy.getInt(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_HEADING else Runways.RECIPROCAL_END_HEADING))
        heading = if (heading > 0) {
            DataUtils.calculateMagneticHeading(heading, mDeclination.roundToInt())
        } else {
            // Actual heading is not available, try to deduce it from runway id
            DataUtils.getRunwayHeading(endId)
        }
        val tv = findViewById<TextView>(
            if (base) R.id.rwy_base_end_label else R.id.rwy_reciprocal_end_label)
        tv!!.text = "Runway $endId"
        val layout = findViewById<LinearLayout>(
            if (base) R.id.rwy_base_end_details else R.id.rwy_reciprocal_end_details)
        addRow(layout!!, "Magnetic heading", FormatUtils.formatDegrees(heading))
        val ilsType = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_ILS_TYPE else Runways.RECIPROCAL_END_ILS_TYPE))
        if (ilsType.isNotEmpty()) {
            val siteNumber = apt.getString(apt.getColumnIndexOrThrow(Airports.SITE_NUMBER))
            val args = Bundle()
            args.putString(Ils1.SITE_NUMBER, siteNumber)
            args.putString(Ils1.RUNWAY_ID, endId)
            args.putString(Ils1.ILS_TYPE, ilsType)
            args.putString(Airports.ICAO_CODE, icaoCode)
            addClickableRow(layout, "ILS type...", ilsType, IlsFragment::class.java, args)
        }
        val elevation = rwy.getFloat(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_RUNWAY_ELEVATION else Runways.RECIPROCAL_END_RUNWAY_ELEVATION))
        if (elevation > 0) {
            addRow(layout, "Elevation", FormatUtils.formatFeet(elevation))
        }
        val rhPattern = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_RIGHT_TRAFFIC else Runways.RECIPROCAL_END_RIGHT_TRAFFIC))
        addRow(layout, "Traffic pattern", if (rhPattern.equals("Y")) "Right" else "Left")
        val gradient = rwy.getDouble(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_GRADIENT else Runways.RECIPROCAL_END_GRADIENT))
        if (gradient > 0) {
            sb.setLength(0)
            sb.append(String.format(Locale.US, "%.1f%%", gradient))
            val gradientDir = rwy.getString(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_GRADIENT_DIRECTION
                else Runways.RECIPROCAL_END_GRADIENT_DIRECTION))
            if (gradientDir.isNotEmpty()) {
                sb.append(" ")
                sb.append(gradientDir)
            }
            addRow(layout, "Gradient", sb.toString())
        }
        val ars = result[if (base) 2 else 3]
        if (ars != null && ars.moveToFirst()) {
            do {
                val arrestingDevice = ars.getString(ars.getColumnIndexOrThrow(Ars.ARRESTING_DEVICE))
                if (arrestingDevice.isNotEmpty()) {
                    addRow(layout, "Arresting device", arrestingDevice)
                }
            } while (ars.moveToNext())
        }
        val apchLights = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_APCH_LIGHT_SYSTEM
            else Runways.RECIPROCAL_END_APCH_LIGHT_SYSTEM))
        if (apchLights.isNotEmpty()) {
            addRow(layout, "Approach lights", apchLights)
        }
        val markings = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_MARKING_TYPE else Runways.RECIPROCAL_END_MARKING_TYPE))
        if (markings.isNotEmpty()) {
            sb.setLength(0)
            sb.append(DataUtils.decodeRunwayMarking(markings))
            val condition = rwy.getString(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_MARKING_CONDITION
                else Runways.RECIPROCAL_END_MARKING_CONDITION))
            if (condition.isNotEmpty()) {
                sb.append(", ")
                sb.append(DataUtils.decodeRunwayMarkingCondition(condition))
            }
            addRow(layout, "Markings", sb.toString())
        }
        val glideSlope = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_VISUAL_GLIDE_SLOPE
            else Runways.RECIPROCAL_END_VISUAL_GLIDE_SLOPE))
        if (glideSlope.isNotEmpty()) {
            addRow(layout, "Glideslope", DataUtils.decodeGlideSlope(glideSlope))
        }
        val glideAngle = rwy.getFloat(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_GLIDE_ANGLE else Runways.RECIPROCAL_END_GLIDE_ANGLE))
        if (glideAngle > 0) {
            addRow(layout, "Glide angle", FormatUtils.formatDegrees(glideAngle))
        }
        val reil = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_REIL_AVAILABLE else Runways.RECIPROCAL_END_REIL_AVAILABLE))
        addRow(layout, "REIL", if (reil.equals("Y")) "Yes" else "No")
        val displacedThreshold = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_DISPLACED_THRESHOLD_LENGTH
                else Runways.RECIPROCAL_END_DISPLACED_THRESHOLD_LENGTH))
        if (displacedThreshold > 0) {
            addRow(layout, "Displaced threshold",
                FormatUtils.formatFeet(displacedThreshold.toFloat()))
        }
        val showExtra = activityBase.prefShowExtraRunwayData
        if (showExtra) {
            val centerline = rwy.getString(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_CENTERLINE_LIGHTS_AVAILABLE
                else Runways.RECIPROCAL_END_CENTERLINE_LIGHTS_AVAILABLE))
            addRow(layout, "Centerline lights", if (centerline == "Y") "Yes" else "No")
            val touchdown = rwy.getString(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_TOUCHDOWN_LIGHTS_AVAILABLE
                else Runways.RECIPROCAL_END_TOUCHDOWN_LIGHTS_AVAILABLE))
            addRow(layout, "Touchdown lights", if (touchdown == "Y") "Yes" else "No")
            val tora = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_TORA else Runways.RECIPROCAL_END_TORA))
            if (tora > 0) {
                addRow(layout, "TORA", FormatUtils.formatFeet(tora.toFloat()))
            }
            val toda = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_TODA else Runways.RECIPROCAL_END_TODA))
            if (toda > 0) {
                addRow(layout, "TODA", FormatUtils.formatFeet(toda.toFloat()))
            }
            val lda = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_LDA else Runways.RECIPROCAL_END_LDA))
            if (lda > 0) {
                addRow(layout, "LDA", FormatUtils.formatFeet(lda.toFloat()))
            }
            val asda = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_ASDA else Runways.RECIPROCAL_END_ASDA))
            if (asda > 0) {
                addRow(layout, "ASDA", FormatUtils.formatFeet(asda.toFloat()))
            }
            val tch = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_THRESHOLD_CROSSING_HEIGHT
                else Runways.RECIPROCAL_END_THRESHOLD_CROSSING_HEIGHT))
            if (tch > 0) {
                addRow(layout, "TCH", FormatUtils.formatFeet(tch.toFloat()))
            }
            val tdz = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_TDZ_ELEVATION
                else Runways.RECIPROCAL_END_TDZ_ELEVATION))
            if (tdz > 0) {
                addRow(layout, "TDZ elevation", FormatUtils.formatFeet(tdz.toFloat()))
            }
            val lahso = rwy.getInt(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_LAHSO_DISTANCE
                else Runways.RECIPROCAL_END_LAHSO_DISTANCE))
            val lahsoRunway = rwy.getString(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_LAHSO_RUNWAY else Runways.RECIPROCAL_END_LAHSO_RUNWAY))
            if (lahso > 0) {
                if (lahsoRunway.isNotEmpty()) {
                    addRow(layout, "LAHSO distance", String.format("%s to %s",
                            FormatUtils.formatFeet(lahso.toFloat()), lahsoRunway))
                } else {
                    addRow(layout, "LAHSO distance", FormatUtils.formatFeet(lahso.toFloat()))
                }
            }
        }
        showRemarks(result, base)
    }

    @SuppressLint("SetTextI18n")
    private fun showHelipadInformation(result: Array<Cursor?>) {
        // Hide the runway sections
        var layout = findViewById<LinearLayout>(R.id.rwy_base_end_details)
        var tv = findViewById<TextView>(R.id.rwy_base_end_label)
        tv!!.visibility = View.GONE
        layout!!.visibility = View.GONE
        tv = findViewById(R.id.rwy_reciprocal_end_label)
        tv!!.visibility = View.GONE
        layout = findViewById(R.id.rwy_reciprocal_end_details)
        layout!!.visibility = View.GONE
        val rwy = result[1]
        val helipadId = rwy!!.getString(rwy.getColumnIndexOrThrow(Runways.RUNWAY_ID))
        tv = findViewById(R.id.rwy_common_label)
        tv!!.text = "Helipad $helipadId"
        layout = findViewById(R.id.rwy_common_details)
        val length = rwy.getInt(rwy.getColumnIndexOrThrow(Runways.RUNWAY_LENGTH))
        val width = rwy.getInt(rwy.getColumnIndexOrThrow(Runways.RUNWAY_WIDTH))
        addRow(layout!!, "Dimensions", String.format("%s x %s",
                FormatUtils.formatFeet(length.toFloat()), FormatUtils.formatFeet(width.toFloat())))
        val surfaceType = rwy.getString(rwy.getColumnIndexOrThrow(Runways.SURFACE_TYPE))
        addRow(layout, "Surface type", DataUtils.decodeSurfaceType(surfaceType))
        val surfaceTreat = rwy.getString(rwy.getColumnIndexOrThrow(Runways.SURFACE_TREATMENT))
        addRow(layout, "Surface treatment", DataUtils.decodeSurfaceTreatment(surfaceTreat))
        val markings = rwy.getString(rwy.getColumnIndexOrThrow(Runways.BASE_END_MARKING_TYPE))
        val condition = rwy.getString(
            rwy.getColumnIndexOrThrow(
                Runways.BASE_END_MARKING_CONDITION
            )
        )
        if (markings.isNotEmpty()) {
            addRow(
                layout, "Markings", DataUtils.decodeRunwayMarking(markings)
                        + ", " + DataUtils.decodeRunwayMarkingCondition(condition)
            )
        }
        val edgeLights = rwy.getString(rwy.getColumnIndexOrThrow(Runways.EDGE_LIGHTS_INTENSITY))
        addRow(layout, "Edge lights", DataUtils.decodeRunwayEdgeLights(edgeLights))
        val rhPattern = rwy.getString(rwy.getColumnIndexOrThrow(Runways.BASE_END_RIGHT_TRAFFIC))
        addRow(layout, "Traffic pattern", if (rhPattern == "Y") "Right" else "Left")

        // Show remarks
        val rmkLayout = findViewById<LinearLayout>(R.id.rwy_base_end_remarks)
        showRemarks(rmkLayout!!, result, helipadId)
    }

    private fun showCommonRemarks(result: Array<Cursor?>, runwayId: String) {
        val rmk = result[4] ?: return
        val layout = findViewById<LinearLayout>(R.id.rwy_common_remarks) ?: return
        var count = 0
        if (rmk.moveToFirst()) {
            do {
                val rmkName = rmk.getString(rmk.getColumnIndexOrThrow(Remarks.REMARK_NAME))
                if (rmkName.startsWith("A81")) {
                    ++count
                    val rmkText = rmk.getString(rmk.getColumnIndexOrThrow(Remarks.REMARK_TEXT))
                    addBulletedRow(layout, rmkText)
                }
            } while (rmk.moveToNext())
        }
        count += showRemarks(layout, result, runwayId)
        if (count > 0) {
            layout.visibility = View.VISIBLE
        }
    }

    private fun showRemarks(result: Array<Cursor?>, base: Boolean) {
        var count = 0
        val rwy = result[1] ?: return
        val layout = findViewById<LinearLayout>(if (base) R.id.rwy_base_end_remarks
            else R.id.rwy_reciprocal_end_remarks) ?: return
        val als = rwy.getString(rwy.getColumnIndexOrThrow(
                if (base) Runways.BASE_END_APCH_LIGHT_SYSTEM
                else Runways.RECIPROCAL_END_APCH_LIGHT_SYSTEM))
        val apchLights = DataUtils.getApproachLightSystemDescription(als).uppercase()
        if (apchLights.isNotEmpty()) {
            addBulletedRow(layout, apchLights)
            ++count
        }

        // Show RVR information
        val rvr = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_RVR_LOCATIONS else Runways.RECIPROCAL_END_RVR_LOCATIONS))
        if (rvr.isNotEmpty()) {
            addBulletedRow(layout, "RVR EQUIP LCTD  AT ${DataUtils.decodeRVRLocations(rvr)}")
            ++count
        }

        // Show obstructions
        count += showObstructions(layout, result, base)
        val runwayId = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_ID else Runways.RECIPROCAL_END_ID))
        showRemarks(layout, result, runwayId)
        if (count > 0) {
            layout.visibility = View.VISIBLE
        }
    }

    private fun showRemarks(layout: LinearLayout, result: Array<Cursor?>, runwayId: String): Int {
        var count = 0
        val rmk = result[4] ?: return count
        if (rmk.moveToFirst()) {
            do {
                val rmkName = rmk.getString(rmk.getColumnIndexOrThrow(Remarks.REMARK_NAME))
                if (rmkName.endsWith("-$runwayId")) {
                    ++count
                    val rmkText = rmk.getString(rmk.getColumnIndexOrThrow(Remarks.REMARK_TEXT))
                    addBulletedRow(layout, rmkText)
                }
            } while (rmk.moveToNext())
        }
        return count
    }

    private fun showObstructions(layout: LinearLayout, result: Array<Cursor?>, base: Boolean): Int {
        val rwy = result[1] ?: return 0
        var count = 0
        val sb = StringBuilder()
        val obstruction = rwy.getString(rwy.getColumnIndexOrThrow(
            if (base) Runways.BASE_END_CONTROLLING_OBJECT
            else Runways.RECIPROCAL_END_CONTROLLING_OBJECT))
        if (obstruction.isNotEmpty()) {
            val height = rwy.getInt(
                if (base) rwy.getColumnIndexOrThrow(Runways.BASE_END_CONTROLLING_OBJECT_HEIGHT)
                else rwy.getColumnIndexOrThrow(Runways.RECIPROCAL_END_CONTROLLING_OBJECT_HEIGHT))
            if (height > 0) {
                val distance = rwy.getInt(rwy.getColumnIndexOrThrow(
                    if (base) Runways.BASE_END_CONTROLLING_OBJECT_DISTANCE
                    else Runways.RECIPROCAL_END_CONTROLLING_OBJECT_DISTANCE))
                val lighted = rwy.getString(rwy.getColumnIndexOrThrow(
                    if (base) Runways.BASE_END_CONTROLLING_OBJECT_LIGHTED
                    else Runways.RECIPROCAL_END_CONTROLLING_OBJECT_LIGHTED))
                val offset = rwy.getString(rwy.getColumnIndexOrThrow(
                    if (base) Runways.BASE_END_CONTROLLING_OBJECT_OFFSET
                    else Runways.RECIPROCAL_END_CONTROLLING_OBJECT_OFFSET))
                val slope = rwy.getInt(rwy.getColumnIndexOrThrow(
                    if (base) Runways.BASE_END_CONTROLLING_OBJECT_SLOPE
                    else Runways.RECIPROCAL_END_CONTROLLING_OBJECT_SLOPE))
                sb.append(String.format("%s %s, ",
                    FormatUtils.formatFeet(height.toFloat()).uppercase(), obstruction))
                if (lighted.isNotEmpty()) {
                    sb.append(DataUtils.decodeControllingObjectLighted(lighted))
                    sb.append(", ")
                }
                sb.append(String.format("%s FROM RWY END",
                    FormatUtils.formatFeet(distance.toFloat()).uppercase()))
                if (offset.isNotEmpty()) {
                    val value = DataUtils.decodeControllingObjectOffset(offset)
                    val dir = DataUtils.decodeControllingObjectOffsetDirection(offset)
                    sb.append(String.format(", %s %s OF CNTRLN",
                        FormatUtils.formatFeet(value.toFloat()), dir))
                }
                if (slope > 0) {
                    sb.append(String.format(", %d:1 SLOPE TO CLEAR", slope))
                }
            } else {
                sb.append(obstruction)
            }
            addBulletedRow(layout, sb.toString())
            ++count
        }
        return count
    }

    private fun doQuery(siteNumber: String, runwayId: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(5)
        cursors[0] = getAirportDetails(siteNumber)
        val apt = cursors[0]
        val lat = apt!!.getDouble(apt.getColumnIndexOrThrow(Airports.REF_LATTITUDE_DEGREES))
        val lon = apt.getDouble(apt.getColumnIndexOrThrow(Airports.REF_LONGITUDE_DEGREES))
        val loc = Location("")
        loc.latitude = lat
        loc.longitude = lon
        mDeclination = GeoUtils.getMagneticDeclination(loc)
        val db = getDatabase(DB_FADDS)
        val builder = SQLiteQueryBuilder()
        builder.tables = Runways.TABLE_NAME
        var c = builder.query(
            db,
            arrayOf("*"),
            Runways.SITE_NUMBER + "=? AND " + Runways.RUNWAY_ID + "=?",
            arrayOf(siteNumber, runwayId),
            null,
            null,
            null,
            null
        )
        c.moveToFirst()
        cursors[1] = c
        try {
            var endId = c.getString(c.getColumnIndexOrThrow(Runways.BASE_END_ID))
            builder.tables = Ars.TABLE_NAME
            c = builder.query(
                db,
                arrayOf("*"),
                Ars.SITE_NUMBER + "=? AND " + Ars.RUNWAY_ID + "=? AND " + Ars.RUNWAY_END_ID + "=?",
                arrayOf(siteNumber, runwayId, endId),
                null,
                null,
                null,
                null
            )
            cursors[2] = c
            endId = c.getString(c.getColumnIndexOrThrow(Runways.RECIPROCAL_END_ID))
            builder.tables = Ars.TABLE_NAME
            c = builder.query(
                db,
                arrayOf("*"),
                Ars.SITE_NUMBER + "=? AND " + Ars.RUNWAY_ID + "=? AND " + Ars.RUNWAY_END_ID + "=?",
                arrayOf(siteNumber, runwayId, endId),
                null,
                null,
                null,
                null
            )
            cursors[3] = c
        } catch (ignored: Exception) {
        }
        builder.tables = Remarks.TABLE_NAME
        c = builder.query(
            db,
            arrayOf(Remarks.REMARK_NAME, Remarks.REMARK_TEXT),
            Runways.SITE_NUMBER + "=? "
                    + "AND ( substr(" + Remarks.REMARK_NAME + ", 1, 2) in ('A2', 'A3', 'A4', 'A5', 'A6')"
                    + "OR " + Remarks.REMARK_NAME + " like 'A81%' )",
            arrayOf(siteNumber),
            null,
            null,
            null,
            null
        )
        cursors[4] = c
        return cursors
    }
}