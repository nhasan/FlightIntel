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
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.AirsigmetDetailItemBinding
import com.nadmm.airports.databinding.AirsigmetDetailViewBinding
import com.nadmm.airports.utils.FormatUtils.formatDegrees
import com.nadmm.airports.utils.FormatUtils.formatFeetRangeMsl
import com.nadmm.airports.utils.GeoUtils.getCardinalDirection
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.wx.AirSigmet.AirSigmetEntry
import com.nadmm.airports.wx.AirSigmetService.Companion.ACTION
import com.nadmm.airports.wx.AirSigmetService.Companion.AIRSIGMET_HOURS_BEFORE
import com.nadmm.airports.wx.AirSigmetService.Companion.AIRSIGMET_RADIUS_NM
import com.nadmm.airports.wx.AirSigmetService.Companion.startAirSigmetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AirSigmetFragment : WxFragmentBase() {
    private var location: Location = Location("")
    private var stationId: String = ""

    private var _binding: AirsigmetDetailViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBroadcastFilter(ACTION)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = AirsigmetDetailViewBinding.inflate(inflater, container, false)
        binding.btnViewGraphic.setOnClickListener { v: View? ->
            val intent = Intent(activity, AirSigmetMapActivity::class.java)
            startActivity(intent)
        }

        return createContentView(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        run()
    }

    override fun handleBroadcast(intent: Intent) {
        val type = intent.getStringExtra(NoaaService.TYPE)
        if (type == NoaaService.TYPE_TEXT) {
            showAirSigmetText(intent)
            isRefreshing = false
        }
    }

    override fun getProduct(): String {
        return "airsigmet"
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        run(true)
    }

    private fun run(refresh: Boolean = false) {
        arguments?.let {
            val stationId = it.getString(NoaaService.STATION_ID) ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(stationId)
                }
                result[0]?.let { c ->
                    setCursor(c, refresh)
                }
            }
        }
    }

    private fun doQuery(stationId: String?): Array<Cursor?> {
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS)

        val builder = SQLiteQueryBuilder()
        builder.tables = Wxs.TABLE_NAME
        val selection = "${Wxs.STATION_ID}=?"
        val c = builder.query(
            db, arrayOf("*"), selection, arrayOf(stationId),
            null, null, null, null
        )
        return arrayOf(c)
    }

    private fun setCursor(c: Cursor, refresh: Boolean) {
        if (c.moveToFirst()) {
            stationId = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
            location = Location("").apply {
                val lat = c.getFloat(c.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
                val lon = c.getFloat(c.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))
                latitude = lat.toDouble()
                longitude = lon.toDouble()
            }
            // Now request the airmet/sigmet
            if (stationId.isNotEmpty() && location.latitude != 0.0 && location.longitude != 0.0) {
                startAirSigmetService(requireActivity(), stationId, location, refresh)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAirSigmetText(intent: Intent) {
        activity ?: return
        binding.apply {
            airsigmetEntriesLayout.removeAllViews()
            wxFetchTime.visibility = View.GONE

            val airSigmet = IntentCompat.getSerializableExtra(intent, NoaaService.RESULT, AirSigmet::class.java)
            if (airSigmet != null) {
                val numEntries = airSigmet.entries.size
                val baseMessage = "within $AIRSIGMET_RADIUS_NM NM of $stationId in last $AIRSIGMET_HOURS_BEFORE hours"

                airsigmetTitleMsg.text = if (airSigmet.entries.isNotEmpty()) {
                    "$numEntries AIR/SIGMETs reported $baseMessage"
                } else {
                    "No AIR/SIGMETs reported $baseMessage"
                }

                if (airSigmet.entries.isNotEmpty()) {
                    airSigmet.entries.forEach { entry ->
                        showAirSigmetEntry(airsigmetEntriesLayout, entry)
                    }
                    // Set fetch time only once after the loop if there are entries
                    wxFetchTime.text = "Fetched on ${TimeUtils.formatDateTime(activityBase, airSigmet.fetchTime)}"
                    wxFetchTime.visibility = View.VISIBLE
                }
            } else {
                airsigmetTitleMsg.text = "Unknown technical error occurred."
            }
        }

        setFragmentContentShown(true)
    }

    private fun showAirSigmetEntry(layout: LinearLayout, entry: AirSigmetEntry) {
        AirsigmetDetailItemBinding.inflate(layoutInflater).apply {
            airsigmetType.text = entry.type
            wxRawAirsigmet.text = entry.rawText
            airsigmetTime.text = TimeUtils.formatDateRange(activityBase, entry.fromTime, entry.toTime)

            if (entry.hazardType?.isNotEmpty() == true) {
                addRow(airsigmetDetails, "Type", entry.hazardType)
            }

            if (entry.hazardSeverity?.isNotEmpty() == true) {
                addRow(airsigmetDetails, "Severity", entry.hazardSeverity)
            }

            if (entry.minAltitudeFeet < Int.Companion.MAX_VALUE
                || entry.maxAltitudeFeet < Int.Companion.MAX_VALUE
            ) {
                addRow(
                    airsigmetDetails, "Altitude", formatFeetRangeMsl(
                        entry.minAltitudeFeet, entry.maxAltitudeFeet
                    )
                )
            }

            if (entry.movementDirDegrees < Int.Companion.MAX_VALUE) {
                val sb = StringBuilder()
                sb.append(
                    String.format(
                        "%s (%s)",
                        formatDegrees(entry.movementDirDegrees),
                        getCardinalDirection(entry.movementDirDegrees.toFloat())
                    )
                )
                if (entry.movementSpeedKnots < Int.Companion.MAX_VALUE) {
                    sb.append(String.format(Locale.US, " at %d knots", entry.movementSpeedKnots))
                }
                addRow(airsigmetDetails, "Movement", sb.toString())
            }

            layout.addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
