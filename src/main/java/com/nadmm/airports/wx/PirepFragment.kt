/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.PirepDetailItemBinding
import com.nadmm.airports.databinding.PirepDetailViewBinding
import com.nadmm.airports.utils.FormatUtils.formatDegrees
import com.nadmm.airports.utils.FormatUtils.formatFeetAgl
import com.nadmm.airports.utils.FormatUtils.formatFeetMsl
import com.nadmm.airports.utils.FormatUtils.formatFeetRangeMsl
import com.nadmm.airports.utils.FormatUtils.formatStatuteMiles
import com.nadmm.airports.utils.FormatUtils.formatTemperature
import com.nadmm.airports.utils.GeoUtils.getCardinalDirection
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.wx.Pirep.PirepEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PirepFragment : WxFragmentBase(NoaaService.ACTION_GET_PIREP) {
    private var _binding: PirepDetailViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = PirepDetailViewBinding.inflate(inflater, container, false)
        return createContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        run()
    }

    override fun handleBroadcast(intent: Intent) {
        val type = intent.getStringExtra(NoaaService.TYPE)
        if (NoaaService.TYPE_TEXT == type) {
            showPirep(intent)
            isRefreshing = false
        }
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        run(true)
    }

    private fun run(refresh: Boolean = false) {
        arguments?.let { args ->
            val stationId = args.getString(NoaaService.STATION_ID) ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(stationId)
                }
                fetchPirep(result[0], refresh)
            }
        }
    }

    private fun doQuery(stationId: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(2)
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS)

        var builder = SQLiteQueryBuilder()
        builder.tables = Wxs.TABLE_NAME
        var c = builder.query(
            db,
            arrayOf("*"),
            "${Wxs.STATION_ID}=?",
            arrayOf(stationId),
            null, null, null, null
        )
        cursors[0] = c

        val wxColumns = arrayOf(
            Awos1.WX_SENSOR_IDENT,
            Awos1.WX_SENSOR_TYPE,
            Awos1.STATION_FREQUENCY,
            Awos1.SECOND_STATION_FREQUENCY,
            Awos1.STATION_PHONE_NUMBER,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE
        )
        builder = SQLiteQueryBuilder()
        builder.setTables(
            ("${Airports.TABLE_NAME} a"
                    + " LEFT JOIN ${Awos1.TABLE_NAME} w"
                    + " ON a.${Airports.FAA_CODE} = w.${Awos1.WX_SENSOR_IDENT}")
        )
        val selection = "a.${Airports.ICAO_CODE}=?"
        c = builder.query(
            db, wxColumns, selection, arrayOf<String?>(stationId),
            null, null, null, null
        )
        cursors[1] = c

        return cursors
    }

    private fun fetchPirep(c: Cursor?, refresh: Boolean) {
        if (c == null || !c.moveToFirst()) {
            arguments?.let { args ->
                val stationId = args.getString(NoaaService.STATION_ID)
                val error = "Unable to get station info for $stationId"
                binding.apply {
                    pirepEntriesLayout.removeAllViews()
                    pirepEntriesLayout.visibility = View.GONE
                    pirepTitleMsg.text = error
                }
                setContentShown(true)
                isRefreshing = false
            }
        } else {
            val location = Location("")
            val lat = c.getFloat(c.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
            val lon = c.getFloat(c.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))
            location.latitude = lat.toDouble()
            location.longitude = lon.toDouble()
            val stationId = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
            PirepService.startService(requireActivity(),  stationId, location, refresh)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPirep(intent: Intent) {
        val pirep = IntentCompat.getSerializableExtra(intent, NoaaService.RESULT, Pirep::class.java) ?: return

        binding.apply {
            val layout = binding.pirepEntriesLayout
            layout.removeAllViews()

            if (pirep.entries.isNotEmpty()) {
                pirepTitleMsg.text = "%d PIREPs reported within %d NM of %s during last %d hours".format(
                    pirep.entries.size, NoaaService2.PIREP_RADIUS_NM,
                    pirep.stationId, NoaaService2.PIREP_HOURS_BEFORE)
                for (entry in pirep.entries) {
                    showPirepEntry(layout, entry)
                }
            } else {
                pirepTitleMsg.text = "No PIREPs reported within %d NM of %s in last %d hours".format(
                    NoaaService2.PIREP_RADIUS_NM, pirep.stationId, NoaaService2.PIREP_HOURS_BEFORE
                )
            }

            wxFetchTime.text = "Fetched on %s".format(
                TimeUtils.formatDateTime(activityBase, pirep.fetchTime)
            )
            wxFetchTime.visibility = View.VISIBLE
        }

        setFragmentContentShown(true)
    }

    @SuppressLint("SetTextI18n")
    private fun showPirepEntry(layout: LinearLayout, entry: PirepEntry) {
        PirepDetailItemBinding.inflate(layoutInflater).apply {
            val dir = getCardinalDirection(entry.bearing.toFloat())
            pirepTitle.text = "${entry.distanceNM} NM $dir"

            if (entry.observationTime < Long.Companion.MAX_VALUE) {
                pirepTitleExtra.text = TimeUtils.formatElapsedTime(entry.observationTime)
            }

            wxRawPirep.text = entry.rawText

            addRow(pirepDetails, "Type", entry.reportType)
            addRow(pirepDetails, "Aircraft", entry.aircraftRef)

            val time = if (!entry.flags.contains(Pirep.Flags.NoTimeStamp)) TimeUtils.formatDateTime(
                activityBase,
                entry.observationTime
            ) else Pirep.Flags.NoTimeStamp.toString()
            addRow(pirepDetails, "Time", time)

            if (entry.altitudeFeetMsl < Int.MAX_VALUE) {
                val altitude =
                    if (entry.flags.contains(Pirep.Flags.AglIndicated)) {
                        formatFeetAgl(entry.altitudeFeetMsl.toFloat())
                    } else {
                        formatFeetMsl(entry.altitudeFeetMsl.toFloat())
                    }
                addRow(pirepDetails, "Altitude", altitude)
            }

            if (entry.visibilitySM < Int.MAX_VALUE) {
                addRow(pirepDetails, "Visibility", formatStatuteMiles(entry.visibilitySM.toFloat()))
            }

            if (entry.tempCelsius < Int.MAX_VALUE) {
                addRow(pirepDetails, "Temperature", formatTemperature(entry.tempCelsius.toFloat()))
            }

            if (entry.windSpeedKnots < Int.MAX_VALUE) {
                addRow(pirepDetails, "Winds", "%s (true) at %d knots".format(
                        formatDegrees(entry.windDirDegrees), entry.windSpeedKnots))
            }

            if (entry.wxList.isNotEmpty()) {
                val wx = entry.wxList.joinToString { wx ->
                    wx.toString()
                }
                addRow(pirepDetails, "Weather", wx)
            }

            for (sky in entry.skyConditions) {
                addSkyConditionRow(pirepDetails, sky)
            }

            for (turbulence in entry.turbulenceConditions) {
                addTurbulenceRow(pirepDetails, turbulence)
            }

            for (icing in entry.icingConditions) {
                addIcingRow(pirepDetails, icing)
            }

            if (!entry.remarks.isEmpty()) {
                addRow(pirepDetails, "Remarks", entry.remarks)
            }

            layout.addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun addSkyConditionRow(details: LinearLayout, sky: Pirep.SkyCondition) {
        val extra = formatFeetRangeMsl(sky.baseFeetMSL, sky.topFeetMSL)
        val skyCondition = SkyCondition.of(sky.skyCover)
        addRow(details, "Sky", skyCondition.toString(), extra)
    }

    private fun addTurbulenceRow(details: LinearLayout, turbulence: Pirep.TurbulenceCondition) {
        val value = listOf(turbulence.frequency, turbulence.intensity, turbulence.type).joinToString(" ")
        val extra = formatFeetRangeMsl(turbulence.baseFeetMSL, turbulence.topFeetMSL)
        addRow(details, "Turbulence", value, extra)
    }

    private fun addIcingRow(details: LinearLayout, icing: Pirep.IcingCondition) {
        val value = listOf(icing.intensity, icing.type).joinToString(" ")
        val extra = formatFeetRangeMsl(icing.baseFeetMSL, icing.topFeetMSL)
        addRow(details, "Icing", value, extra)
    }
}
