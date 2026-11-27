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
package com.nadmm.airports.wx

import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.WindsAloftViewBinding
import com.nadmm.airports.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Winds and temperatures aloft over the station, now and 6, 12 and 24 hours
 * ahead, at the altitudes of the retired FB forecasts.
 */
class WindsAloftFragment : WxFragmentBase(NoaaService.ACTION_GET_WINDS_ALOFT) {
    private var _binding: WindsAloftViewBinding? = null
    private val binding get() = _binding!!
    // What is showing, so coming back to the tab does not redraw the same
    private var shownFetchTime = 0L
    // The station's header and position, found once per view
    private var station: StationInfo? = null

    private class StationInfo(val location: Location, val elevationFt: Int)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = WindsAloftViewBinding.inflate(inflater, container, false)
        return createContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        fetchWindsAloft()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownFetchTime = 0L
        station = null
        _binding = null
    }

    override fun isRefreshable() = true

    override fun requestDataRefresh() = fetchWindsAloft(true)

    override fun processResult(result: NoaaService.Result) {
        if (result.type == NoaaService.TYPE_TEXT) {
            val winds = getResultObject(result, WindsAloft::class.java)
            if (winds.fetchTime == 0L || winds.fetchTime != shownFetchTime) {
                shownFetchTime = winds.fetchTime
                showWindsAloft(winds)
            }
            isRefreshing = false
        }
    }

    private fun fetchWindsAloft(refresh: Boolean = false) {
        val stationId = arguments?.getString(NoaaService.STATION_ID) ?: return
        station?.let {
            WindsAloftService.startService(requireActivity(), stationId, it.location,
                it.elevationFt, refresh)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val cursors = withContext(Dispatchers.IO) { queryStation(stationId) }
            val wxs = cursors[0]
            if (wxs == null || !wxs.moveToFirst()) {
                showMessage("Unable to get station info for $stationId")
                return@launch
            }
            wxs.moveToFirst()
            val location = Location("").apply {
                latitude = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
                longitude = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))
            }
            val elevationM = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_ELEVATOIN_METER))
            val info = StationInfo(location, (elevationM * FEET_PER_METER).roundToInt())
            this@WindsAloftFragment.station = info
            WindsAloftService.startService(requireActivity(), stationId, info.location,
                info.elevationFt, refresh)
        }
    }

    /** The station, and its AWOS and airport details for the title. */
    private fun queryStation(stationId: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(2)
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS) ?: return cursors
        cursors[0] = SQLiteQueryBuilder().apply { tables = Wxs.TABLE_NAME }.query(
            db, arrayOf("*"), "${Wxs.STATION_ID}=?", arrayOf(stationId),
            null, null, null, null)
        cursors[1] = SQLiteQueryBuilder().apply {
            tables = "${Airports.TABLE_NAME} a LEFT JOIN ${Awos1.TABLE_NAME} w" +
                    " ON a.${Airports.FAA_CODE} = w.${Awos1.WX_SENSOR_IDENT}"
        }.query(
            db,
            arrayOf(Awos1.WX_SENSOR_IDENT, Awos1.WX_SENSOR_TYPE, Awos1.STATION_FREQUENCY,
                Awos1.SECOND_STATION_FREQUENCY, Awos1.STATION_PHONE_NUMBER,
                Airports.ASSOC_CITY, Airports.ASSOC_STATE),
            "a.${Airports.ICAO_CODE}=?", arrayOf(stationId), null, null, null, null)
        return cursors
    }

    private fun showMessage(message: String) {
        binding.windsAloftForecasts.removeAllViews()
        binding.wxReportNote.visibility = View.GONE
        binding.windsAloftMsg.text = message
        binding.windsAloftMsg.visibility = View.VISIBLE
        setFragmentContentShown(true)
        isRefreshing = false
    }

    private fun showWindsAloft(winds: WindsAloft) {
        if (winds.forecasts.isEmpty()) {
            showMessage("Winds aloft are not available right now.")
            return
        }
        binding.windsAloftMsg.visibility = View.GONE
        val layout = binding.windsAloftForecasts
        layout.removeAllViews()
        for (forecast in winds.forecasts) {
            val label = inflate<TextView>(R.layout.tfr_detail_label)
            val time = TimeUtils.formatDateTime(activityBase, forecast.timeMillis)
            label.text = "${whenLabel(forecast.timeMillis)}, $time"
            layout.addView(label)
            val card = inflate<LinearLayout>(R.layout.tfr_detail_card)
            layout.addView(card)
            addColumns(card, "Altitude", "Wind", "Temp", header = true)
            for (row in forecast.rows) {
                addColumns(card, String.format(Locale.US, "%,d ft", row.altitudeFt),
                    formatWind(row), formatTemperature(row))
            }
        }
        binding.wxReportNote.text = "Fetched ${TimeUtils.formatElapsedTime(winds.fetchTime)}"
        binding.wxReportNote.visibility = View.VISIBLE
        setFragmentContentShown(true)
    }

    /**
     * "Now" for the forecast hour under way, else how far ahead it is, from
     * the time shown rather than when the forecast was fetched.
     */
    private fun whenLabel(timeMillis: Long): String {
        val ahead = timeMillis - System.currentTimeMillis()
        return when {
            ahead <= -HOUR_MILLIS -> "Earlier"
            ahead <= 0 -> "Now"
            else -> {
                val hours = (ahead + HOUR_MILLIS - 1) / HOUR_MILLIS
                if (hours == 1L) "In 1 hour" else "In $hours hours"
            }
        }
    }

    /** A row of the three columns; the heading row in the lighter style. */
    private fun addColumns(card: LinearLayout, altitude: String, wind: String, temp: String,
                           header: Boolean = false) {
        val row = inflate<LinearLayout>(R.layout.winds_aloft_row)
        val views = listOf(R.id.winds_altitude to altitude, R.id.winds_wind to wind,
            R.id.winds_temperature to temp)
        for ((id, text) in views) {
            row.findViewById<TextView>(id).apply {
                this.text = text
                if (header) TextViewCompat.setTextAppearance(this, R.style.TextSmall_Light)
            }
        }
        if (header) {
            // A lighter, shorter row, so it reads as a heading
            val padding = resources.getDimensionPixelSize(R.dimen.label_padding)
            row.setPadding(row.paddingLeft, padding * 2, row.paddingRight, padding * 2)
        }
        addRow(card, row)
    }

    /** Such as "250° 31 kt", "Light and variable", or "—" when not forecast. */
    private fun formatWind(row: WindsAloftRow): String = when {
        !row.hasWind -> "—"
        row.lightAndVariable -> "Light and variable"
        row.directionDeg != null && row.speedKt != null ->
            String.format(Locale.US, "%03d° %d kt", row.directionDeg, row.speedKt)
        else -> "—"
    }

    /** Such as "−3°C", or empty when not forecast. */
    private fun formatTemperature(row: WindsAloftRow): String = row.temperatureC?.let {
        if (it == 0) "0°C" else String.format(Locale.US, "%s%d°C", if (it < 0) "−" else "+", abs(it))
    } ?: ""

    companion object {
        private const val FEET_PER_METER = 3.28084
        private const val HOUR_MILLIS = 60 * 60 * 1000L
    }
}
