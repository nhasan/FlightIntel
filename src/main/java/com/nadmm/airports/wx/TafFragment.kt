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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.GroupedDetailItemBinding
import com.nadmm.airports.databinding.TafDetailViewBinding
import com.nadmm.airports.utils.DbUtils
import com.nadmm.airports.utils.FormatUtils.formatAltimeter
import com.nadmm.airports.utils.FormatUtils.formatDegrees
import com.nadmm.airports.utils.FormatUtils.formatFeetAgl
import com.nadmm.airports.utils.FormatUtils.formatFeetRangeAgl
import com.nadmm.airports.utils.FormatUtils.formatStatuteMiles
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.GeoUtils.getCardinalDirection
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.WxUtils.computeFlightCategory
import com.nadmm.airports.utils.WxUtils.decodeIcingIntensity
import com.nadmm.airports.utils.WxUtils.decodeTurbulenceIntensity
import com.nadmm.airports.utils.WxUtils.setFlightCategoryDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TafFragment : WxFragmentBase(NoaaService.ACTION_GET_TAF) {
    private var _binding: TafDetailViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = TafDetailViewBinding.inflate(inflater, container, false)
        val view = binding.root
        return createContentView(view)
    }

    override fun onResume() {
        super.onResume()

        fetchTaf()
    }

    override fun handleBroadcast(intent: Intent) {
        val type = intent.getStringExtra(NoaaService.TYPE)
        if (NoaaService.TYPE_TEXT == type) {
            showTaf(intent)
            isRefreshing = false
        }
    }

    override fun isRefreshable()  = true

    override fun requestDataRefresh() = fetchTaf(true)

    private fun fetchTaf(refresh: Boolean = false) {
        arguments?.let {
            val stationId = arguments?.getString(NoaaService.STATION_ID) ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(stationId)
                }
                fetchTaf(result, refresh)
            }
        }
    }

    private fun fetchTaf(result: Array<Cursor?>, refresh: Boolean) {
        val wxs = result[0]
        if (wxs?.moveToFirst() == true) {
            // We have a station with TAF, get the station id
            val stationId = wxs.getString(wxs.getColumnIndexOrThrow(Wxs.STATION_ID))
            showWxTitle(binding.wxTitle, binding.wxSubtitle, result)
            TafService.startService(requireActivity(), stationId, refresh)
        } else {
            // No station with TAF was found
            showError()
        }
        for (c in result) {
            c?.close()
        }
    }

    private fun findTafStationId(db: SQLiteDatabase, stationId: String): String? {
        var tafStationId = null as String?

        val builder = SQLiteQueryBuilder()
        builder.tables = Wxs.TABLE_NAME
        val selection = Wxs.STATION_ID + "=?"
        var c = builder.query(
            db, arrayOf("*"), selection,
            arrayOf(stationId), null, null, null, null
        )
        c.moveToFirst()
        var siteTypes = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_SITE_TYPES))
        if (siteTypes.contains("TAF")) {
            // There is a TAF available at this station
            tafStationId = stationId
        } else {
            // There is no TAF available at this station, search for the nearest
            val lat = c.getDouble(c.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
            val lon = c.getDouble(c.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))
            val location = Location("")
            location.latitude = lat
            location.longitude = lon
            c.close()

            c = DbUtils.getBoundingBoxCursor(
                db, Wxs.TABLE_NAME,
                Wxs.STATION_LATITUDE_DEGREES, Wxs.STATION_LONGITUDE_DEGREES,
                location, NoaaService.TAF_RADIUS
            )
            if (c.moveToFirst()) {
                var distance = Float.MAX_VALUE
                do {
                    siteTypes = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_SITE_TYPES))
                    if (!siteTypes.contains("TAF")) {
                        continue
                    }
                    // Get the location of this station
                    val results = FloatArray(2)
                    Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        c.getDouble(c.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES)),
                        c.getDouble(c.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES)),
                        results
                    )
                    results[0] /= GeoUtils.METERS_PER_NAUTICAL_MILE
                    if (results[0] <= NoaaService.TAF_RADIUS && results[0] < distance) {
                        tafStationId = c.getString(c.getColumnIndexOrThrow(Wxs.STATION_ID))
                        distance = results[0]
                    }
                } while (c.moveToNext())
            }
        }
        c.close()

        return tafStationId
    }

    private fun getStationDataCursors(db: SQLiteDatabase, stationId: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(2)
        var builder = SQLiteQueryBuilder()
        builder.tables = Wxs.TABLE_NAME
        var selection = Wxs.STATION_ID + "=?"
        var c = builder.query(
            db, arrayOf("*"), selection,
            arrayOf(stationId), null, null, null, null
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
        builder.tables = (Airports.TABLE_NAME + " a"
                + " LEFT JOIN " + Awos1.TABLE_NAME + " w"
                + " ON a." + Airports.FAA_CODE + " = w." + Awos1.WX_SENSOR_IDENT)
        selection = "a." + Airports.ICAO_CODE + "=?"
        c = builder.query(
            db, wxColumns, selection, arrayOf(stationId),
            null, null, null, null
        )
        cursors[1] = c

        return cursors
    }

    private fun doQuery(stationId: String): Array<Cursor?> {
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS)

        val tafStationId = findTafStationId(db, stationId) ?: return arrayOfNulls<Cursor>(2)
        // We have the station with TAF
        return getStationDataCursors(db, tafStationId)
    }

    @SuppressLint("SetTextI18n")
    private fun showError() {
        arguments?.let {
            with(binding) {
                val stationId = it.getString(NoaaService.STATION_ID) ?: return
                // No station with TAF was found nearby
                wxDetailLayout.visibility = View.GONE
                wxStatusLayout.removeAllViews()
                wxStatusLayout.visibility = View.GONE
                statusMsg.visibility = View.VISIBLE
                statusMsg.text =
                    "No wx station with TAF was found near $stationId within ${NoaaService.TAF_RADIUS}NM radius"
                wxTitle.wxTitleLayout.visibility = View.GONE
            }
            isRefreshing = false
            setContentShown(true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showTaf(intent: Intent) {
        with(binding) {
            val taf = IntentCompat.getParcelableExtra<Taf>(intent, NoaaService.RESULT, Taf::class.java)
            wxStatusLayout.removeAllViews()
            if (taf?.isValid != true) {
                statusMsg.text = "Unable to get TAF for this location."
                with(wxStatusLayout) {
                    addRow(this, "This could be due to the following reasons:")
                    addBulletedRow(this, "Network connection is not available")
                    addBulletedRow(this, "ADDS does not publish TAF for this station")
                    addBulletedRow(this, "Station is currently out of service")
                    addBulletedRow(this, "Station has not updated the TAF for more than 12 hours")
                }
                statusMsg.visibility = View.VISIBLE
                wxStatusLayout.visibility = View.VISIBLE
                wxDetailLayout.visibility = View.GONE
                setFragmentContentShown(true)
                return
            } else {
                statusMsg.text = ""
                statusMsg.visibility = View.GONE
                wxStatusLayout.visibility = View.GONE
                wxDetailLayout.visibility = View.VISIBLE
            }

            wxSubtitle.wxAge.text = TimeUtils.formatElapsedTime(taf.issueTime)
            // Raw Text
            wxRawTaf.text = taf.rawText?.replace("(FM|BECMG|TEMPO)".toRegex(), "\n    $1")

            tafSummaryLayout.removeAllViews()
            val fcstType = if (taf.rawText?.startsWith("TAF AMD ") == true) {
                "Amendment"
            } else if (taf.rawText?.startsWith("TAF COR ") == true) {
                "Correction"
            } else {
                "Normal"
            }
            addRow(tafSummaryLayout, "Forecast type", fcstType)
            addRow(
                tafSummaryLayout, "Issued at",
                TimeUtils.formatDateTime(activityBase, taf.issueTime)
            )
            addRow(
                tafSummaryLayout, "Valid from",
                TimeUtils.formatDateTime(activityBase, taf.validTimeFrom)
            )
            addRow(
                tafSummaryLayout, "Valid to",
                TimeUtils.formatDateTime(activityBase, taf.validTimeTo)
            )
            taf.remarks?.let {
                if (it.isNotEmpty() && it != "AMD") {
                    addRow(tafSummaryLayout, "\u2022 $it")
                }
            }

            tafForecastsLayout.removeAllViews()
            var lastForecast: Forecast? = null
            val sb = StringBuilder()
            for (forecast in taf.forecasts) {
                // Keep track of forecast conditions across all change groups
                if (lastForecast == null || forecast.changeIndicator == null || forecast.changeIndicator == "FM") {
                    lastForecast = forecast
                } else {
                    if (forecast.visibilitySM < Float.MAX_VALUE) {
                        lastForecast.visibilitySM = forecast.visibilitySM
                    }
                    if (forecast.skyConditions.isNotEmpty()) {
                        lastForecast.skyConditions = forecast.skyConditions
                    }
                }

                sb.setLength(0)
                if (forecast.changeIndicator != null) {
                    sb.append(forecast.changeIndicator)
                    sb.append(" ")
                }
                sb.append(
                    TimeUtils.formatDateRange(
                        activityBase,
                        forecast.timeFrom, forecast.timeTo
                    )
                )
                val layout = inflate<RelativeLayout>(R.layout.grouped_detail_item)
                with(GroupedDetailItemBinding.bind(layout)) {
                    groupExtra.visibility = View.GONE
                    groupName.text = sb.toString()

                    val flightCategory = computeFlightCategory(
                        lastForecast.skyConditions, lastForecast.visibilitySM
                    )
                    setFlightCategoryDrawable(groupName, flightCategory)

                    if (forecast.probability < Int.MAX_VALUE) {
                        addRow(
                            groupDetails, "Probability", String.format(
                                Locale.US,
                                "%d%%", forecast.probability
                            )
                        )
                    }

                    if ((forecast.changeIndicator ?: "") == "BECMG") {
                        addRow(
                            groupDetails, "Becoming at", TimeUtils.formatDateTime(
                                activityBase, forecast.timeBecoming
                            )
                        )
                    }

                    if (forecast.windSpeedKnots < Int.MAX_VALUE) {
                        val wind = if (forecast.windDirDegrees == 0 && forecast.windSpeedKnots == 0) {
                            "Calm"
                        } else if (forecast.windDirDegrees == 0) {
                            String.format(
                                Locale.US, "Variable at %d knots",
                                forecast.windSpeedKnots
                            )
                        } else {
                            String.format(
                                Locale.US, "%s (%s true) at %d knots",
                                getCardinalDirection(forecast.windDirDegrees.toFloat()),
                                formatDegrees(forecast.windDirDegrees),
                                forecast.windSpeedKnots
                            )
                        }
                        var gust = ""
                        if (forecast.windGustKnots < Int.MAX_VALUE) {
                            gust = String.format(
                                Locale.US,
                                "Gusting to %d knots", forecast.windGustKnots
                            )
                        }
                        addRow(groupDetails, "Winds", wind, gust)
                    }

                    if (forecast.visibilitySM < Float.MAX_VALUE) {
                        val value = if (forecast.visibilitySM > 6)
                            "6+ SM"
                        else
                            formatStatuteMiles(forecast.visibilitySM)
                        addRow(groupDetails, "Visibility", value)
                    }

                    if (forecast.vertVisibilityFeet < Int.MAX_VALUE) {
                        addRow(
                            groupDetails, "Visibility",
                            formatFeetAgl(forecast.vertVisibilityFeet.toFloat())
                        )
                    }

                    for (wx in forecast.wxList) {
                        addRow(groupDetails, "Weather", wx.toString())
                    }

                    for (sky in forecast.skyConditions) {
                        addRow(groupDetails, "Clouds", sky.toString())
                    }

                    if (forecast.windShearSpeedKnots < Int.MAX_VALUE) {
                        val shear = String.format(
                            Locale.US, "%s (%s true) at %d knots",
                            getCardinalDirection(forecast.windShearDirDegrees.toFloat()),
                            formatDegrees(forecast.windShearDirDegrees),
                            forecast.windShearSpeedKnots
                        )
                        val height = formatFeetAgl(forecast.windShearHeightFeetAGL.toFloat())
                        addRow(groupDetails, "Wind shear", shear, height)
                    }

                    if (forecast.altimeterHg < Float.MAX_VALUE) {
                        addRow(
                            groupDetails, "Altimeter",
                            formatAltimeter(forecast.altimeterHg)
                        )
                    }

                    for (turbulence in forecast.turbulenceConditions) {
                        val value = decodeTurbulenceIntensity(turbulence.intensity)
                        val height = formatFeetRangeAgl(
                            turbulence.minAltitudeFeetAGL, turbulence.maxAltitudeFeetAGL
                        )
                        addRow(groupDetails, "Turbulence", value, height)
                    }

                    for (icing in forecast.icingConditions) {
                        val value = decodeIcingIntensity(icing.intensity)
                        val height = formatFeetRangeAgl(
                            icing.minAltitudeFeetAGL, icing.maxAltitudeFeetAGL
                        )
                        addRow(groupDetails, "Icing", value, height)
                    }
                }

                tafForecastsLayout.addView(
                    layout,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            wxFetchTime.text = String.format(
                Locale.US, "Fetched on %s",
                TimeUtils.formatDateTime(activityBase, taf.fetchTime)
            )
            wxFetchTime.visibility = View.VISIBLE

            setFragmentContentShown(true)
        }
    }
}
