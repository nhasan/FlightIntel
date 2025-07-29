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
import androidx.core.view.isNotEmpty
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Awos2
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.DetailRowItem2Binding
import com.nadmm.airports.databinding.MetarDetailViewBinding
import com.nadmm.airports.utils.FormatUtils.formatAltimeter
import com.nadmm.airports.utils.FormatUtils.formatDegrees
import com.nadmm.airports.utils.FormatUtils.formatFeet
import com.nadmm.airports.utils.FormatUtils.formatFeetAgl
import com.nadmm.airports.utils.FormatUtils.formatNumber
import com.nadmm.airports.utils.FormatUtils.formatTemperature
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.UiUtils.setTextViewDrawable
import com.nadmm.airports.utils.UiUtils.showToast
import com.nadmm.airports.utils.WxUtils.getCeiling
import com.nadmm.airports.utils.WxUtils.getDensityAltitude
import com.nadmm.airports.utils.WxUtils.getPressureAltitude
import com.nadmm.airports.utils.WxUtils.getRelativeHumidity
import com.nadmm.airports.utils.WxUtils.getWindBarbDrawable
import com.nadmm.airports.utils.WxUtils.setFlightCategoryDrawable
import com.nadmm.airports.utils.WxUtils.showColorizedDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class MetarFragment : WxFragmentBase() {
    private val action = NoaaService.ACTION_GET_METAR
    private var location: Location? = null
    private var _binding: MetarDetailViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBroadcastFilter(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MetarDetailViewBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.btnViewGraphic.setOnClickListener { _: View? ->
            val intent = Intent(activity, MetarMapActivity::class.java)
            startActivity(intent)
        }
        binding.btnViewGraphic.visibility = View.GONE
        return createContentView(view)
    }

    override fun onResume() {
        super.onResume()

        run()
    }

    override fun handleBroadcast(intent: Intent) {
        if (location == null) {
            // This was probably intended for wx list view
            return
        }
        val type = intent.getStringExtra(NoaaService.TYPE)
        if (NoaaService.TYPE_TEXT == type) {
            showMetar(intent)
            isRefreshing = false
        }
    }

    override fun getProduct(): String {
        return "metar"
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
                fetchMetar(result, refresh)
            }
        }
    }

    private fun doQuery(stationId: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(3)
        val db = activityBase.getDatabase(DatabaseManager.DB_FADDS)
        var builder = SQLiteQueryBuilder()
        builder.tables = Wxs.TABLE_NAME
        var c = builder.query(
            db,
            arrayOf("*"),
            Wxs.STATION_ID + "=?",
            arrayOf(stationId),
            null,
            null,
            null,
            null
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
        var selection =
            "a." + Airports.ICAO_CODE + "=? AND w." + Awos1.COMMISSIONING_STATUS + "='Y'"
        c = builder.query(
            db, wxColumns, selection, arrayOf(stationId),
            null, null, null
        )
        cursors[1] = c
        if (c.moveToFirst()) {
            val sensorId = c.getString(c.getColumnIndexOrThrow(Awos1.WX_SENSOR_IDENT))
            val sensorType = c.getString(c.getColumnIndexOrThrow(Awos1.WX_SENSOR_TYPE))
            builder = SQLiteQueryBuilder()
            builder.tables = Awos2.TABLE_NAME
            selection = String.format(
                "%s=? AND %s=?",
                Awos2.WX_SENSOR_IDENT, Awos2.WX_SENSOR_TYPE
            )
            c = builder.query(
                db, arrayOf(Awos2.WX_STATION_REMARKS),
                selection, arrayOf(sensorId, sensorType), null, null, null
            )
            cursors[2] = c
        }
        return cursors
    }

    private fun fetchMetar(result: Array<Cursor?>, refresh: Boolean) {
        val wxs = result[0]
        if (wxs?.moveToFirst() == true) {
            location = Location("").apply {
                latitude = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
                longitude = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))
            }

            showWxTitle(result)

            // Remarks
            binding.wxRemarksLayout.removeAllViews()
            result[2]?.let { rmk ->
                if (rmk.moveToFirst()) {
                    do {
                        val remark = rmk.getString(rmk.getColumnIndexOrThrow(Awos2.WX_STATION_REMARKS))
                        addBulletedRow(binding.wxRemarksLayout, remark)
                    } while (rmk.moveToNext())
                }
            }

            requestMetar(refresh)
        } else {
            showToast(
                requireActivity().applicationContext,
                "Unable to get weather station info"
            )
            requireActivity().finish()
        }
    }

    private fun requestMetar(refresh: Boolean) {
        arguments?.let { args ->
            args.getString(NoaaService.STATION_ID)?.let { stationId ->
                MetarService.startMetarService(requireActivity(), stationId, refresh)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMetar(intent: Intent) {
        val metar = IntentCompat.getSerializableExtra(intent, NoaaService.RESULT, Metar::class.java) ?: return
        binding.apply {
            wxStatusLayout.removeAllViews()
            if (!metar.isValid) {
                wxStatusLayout.visibility = View.VISIBLE
                statusMsg.visibility = View.VISIBLE
                statusMsg.text = "Unable to get METAR for this location"
                addRow(wxStatusLayout, "This could be due to the following reasons:")
                addBulletedRow(wxStatusLayout, "Network connection is not available")
                addBulletedRow(wxStatusLayout, "AWS does not publish METAR for this station")
                addBulletedRow(wxStatusLayout, "Station is currently out of service")
                addBulletedRow(wxStatusLayout, "Station has not updated the METAR for more than 3 hours")
                wxDetailLayout.visibility = View.GONE
                setFragmentContentShown(true)
                return
            } else {
                statusMsg.visibility = View.GONE
                wxStatusLayout.visibility = View.GONE
                wxDetailLayout.visibility = View.VISIBLE
            }
            setFlightCategoryDrawable(wxTitle.wxStationInfo, metar.flightCategory)
            wxSubtitle.wxAge.text = TimeUtils.formatElapsedTime(metar.observationTime)
            wxRawMetar.text = metar.rawText

            // Winds
            wxWindLayout.removeAllViews()
            if (metar.windSpeedKnots < Int.MAX_VALUE) {
                showWindInfo(wxWindLayout, metar)
            }
            wxWindLayout.visibility = if (wxWindLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxWindLabel.visibility = wxWindLayout.visibility

            // Visibility
            wxVisLayout.removeAllViews()
            if (metar.visibilitySM < Float.MAX_VALUE) {
                if (metar.flags.contains(Metar.Flags.Auto) && metar.visibilitySM >= 10f) {
                    addRow(wxVisLayout, "10+ statute miles horizontal")
                } else {
                    addRow(
                        wxVisLayout, String.format(
                            "%s statute miles horizontal",
                            formatNumber(metar.visibilitySM)
                        )
                    )
                }
                if (metar.vertVisibilityFeet < Int.MAX_VALUE) {
                    addRow(
                        wxVisLayout, String.format(
                            "%s vertical",
                            formatFeetAgl(metar.vertVisibilityFeet.toFloat())
                        )
                    )
                }
            }
            wxVisLabel.visibility = if (wxVisLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxVisLayout.visibility = wxVisLabel.visibility

            // Weather
            wxWeatherLayout.removeAllViews()
            for (wx in metar.wxList) {
                addWeatherRow(wxWeatherLayout, wx, metar.flightCategory)
            }
            if (metar.ltg) {
                addRow(wxWeatherLayout, "Lightning in the vicinity")
            }
            wxWeatherLayout.visibility = if (wxWeatherLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxWeatherLabel.visibility = wxWeatherLayout.visibility

            // Sky Condition
            wxSkyCondLayout.removeAllViews()
            if (metar.skyConditions.isNotEmpty()) {
                val ceiling = getCeiling(metar.skyConditions)
                if (!listOf("NSC", "OVX").contains(ceiling.skyCover)) {
                    addRow(wxSkyCondLayout, "Ceiling is " + formatFeetAgl(ceiling.cloudBaseAGL.toFloat()))
                }
                for (sky in metar.skyConditions) {
                    addSkyConditionRow(wxSkyCondLayout, sky, metar.flightCategory)
                }
            }
            wxSkyCondLayout.visibility = if (wxSkyCondLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxSkyCondLabel.visibility = wxSkyCondLayout.visibility

            // Temperature
            wxTempLayout.removeAllViews()
            if (metar.tempCelsius < Float.MAX_VALUE && metar.dewpointCelsius < Float.MAX_VALUE) {
                addRow(wxTempLayout, "Temperature", formatTemperature(metar.tempCelsius))
                if (metar.dewpointCelsius < Float.MAX_VALUE) {
                    addRow(wxTempLayout, "Dew point", formatTemperature(metar.dewpointCelsius))
                    addRow(
                        wxTempLayout, "Relative humidity",
                        String.format(Locale.US, "%.0f%%", getRelativeHumidity(metar))
                    )
                    val denAlt = getDensityAltitude(metar).toLong()
                    addRow(wxTempLayout, "Density altitude", formatFeet(denAlt.toFloat()))
                } else {
                    addRow(wxTempLayout, "Dew point", "n/a")
                }
                if (metar.maxTemp6HrCentigrade < Float.MAX_VALUE) {
                    addRow(wxTempLayout, "6-hr max", formatTemperature(metar.maxTemp6HrCentigrade))
                }
                if (metar.minTemp6HrCentigrade < Float.MAX_VALUE) {
                    addRow(wxTempLayout, "6-hr min", formatTemperature(metar.minTemp6HrCentigrade))
                }
                if (metar.maxTemp24HrCentigrade < Float.MAX_VALUE) {
                    addRow(wxTempLayout, "24-hr max", formatTemperature(metar.maxTemp24HrCentigrade))
                }
                if (metar.minTemp24HrCentigrade < Float.MAX_VALUE) {
                    addRow(wxTempLayout, "24-hr min", formatTemperature(metar.minTemp24HrCentigrade))
                }
            }
            wxTempLayout.visibility = if (wxTempLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxTempLabel.visibility = wxTempLayout.visibility

            // Pressure
            wxPressureLayout.removeAllViews()
            if (metar.altimeterHg < Float.MAX_VALUE) {
                addRow(wxPressureLayout, "Altimeter", formatAltimeter(metar.altimeterHg))
                if (metar.seaLevelPressureMb < Float.MAX_VALUE) {
                    val slp = formatNumber(metar.seaLevelPressureMb)
                    addRow(wxPressureLayout, "Sea level pressure", "$slp mb")
                }
                val presAlt = getPressureAltitude(metar).toLong()
                addRow(wxPressureLayout, "Pressure altitude", formatFeet(presAlt.toFloat()))
                if (metar.pressureTend3HrMb < Float.MAX_VALUE) {
                    addRow(
                        wxPressureLayout, "3-hr tendency",
                        String.format(Locale.US, "%+.2f mb", metar.pressureTend3HrMb)
                    )
                }
                if (metar.presfr) {
                    addRow(wxPressureLayout, "Pressure is falling rapidly")
                }
                if (metar.presrr) {
                    addRow(wxPressureLayout, "Pressure is rising rapidly")
                }
            }
            wxPressureLayout.visibility = if (wxPressureLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxPressureLabel.visibility = wxPressureLayout.visibility

            // Precipitation
            wxPrecipLayout.removeAllViews()
            if (metar.precipInches < Float.MAX_VALUE) {
                addRow(
                    wxPrecipLayout, "1-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precipInches)
                )
            }
            if (metar.precip3HrInches < Float.MAX_VALUE) {
                addRow(
                    wxPrecipLayout, "3-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precip3HrInches)
                )
            }
            if (metar.precip6HrInches < Float.MAX_VALUE) {
                addRow(
                    wxPrecipLayout, "6-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precip6HrInches)
                )
            }
            if (metar.precip24HrInches < Float.MAX_VALUE) {
                addRow(
                    wxPrecipLayout, "24-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precip24HrInches)
                )
            }
            if (metar.snowInches < Float.MAX_VALUE) {
                addRow(wxPrecipLayout, "Snow depth", String.format(Locale.US, "%.0f\"", metar.snowInches))
            }
            if (metar.snincr) {
                addRow(wxPrecipLayout, "Snow is increasing rapidly")
            }
            wxPrecipLayout.visibility = if (wxPrecipLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxPrecipLabel.visibility = wxPrecipLayout.visibility

            // Remarks
            for (flag in metar.flags) {
                addBulletedRow(wxRemarksLayout, flag.toString())
            }
            wxRemarksLayout.visibility = if (wxRemarksLayout.isNotEmpty()) View.VISIBLE else View.GONE
            wxRemarksLabel.visibility = wxRemarksLayout.visibility

            // Fetch time
            val fetched = TimeUtils.formatDateTime(activityBase, metar.fetchTime)
            wxFetchTime.text = "Fetched on $fetched"
            wxFetchTime.visibility = View.VISIBLE
        }
        setFragmentContentShown(true)
    }

    private fun getWindsDescription(metar: Metar): String {
        val s = StringBuilder()
        if (metar.windDirDegrees == 0 && metar.windSpeedKnots == 0) {
            s.append("Winds are calm")
        } else {
            if (metar.windDirDegrees == 0) {
                s.append("Winds variable at ${metar.windSpeedKnots} kt")
            } else {
                val card = GeoUtils.getCardinalDirection(metar.windDirDegrees.toFloat())
                val dir = formatDegrees(metar.windDirDegrees)
                s.append("From $card ($dir true) at ${metar.windSpeedKnots} kt")
            }
            if (metar.windGustKnots < Int.MAX_VALUE)
                s.append(" gusting to ${metar.windGustKnots} kt")
            if (metar.windPeakKnots < Int.MAX_VALUE && metar.windPeakKnots > metar.windGustKnots)
                s.append(", peak at ${metar.windPeakKnots} kt")
        }
        return s.toString()
    }

    private fun showWindInfo(layout: LinearLayout, metar: Metar) {
        val row = addRow(layout, getWindsDescription(metar))
        DetailRowItem2Binding.bind(row).apply {
            if (metar.windDirDegrees > 0) {
                val declination = location?.let { GeoUtils.getMagneticDeclination(it) } ?: 0F
                val wind = getWindBarbDrawable(requireContext(), metar, declination)
                setTextViewDrawable(itemLabel, wind)
            }
            if (metar.windGustKnots < Int.MAX_VALUE) {
                val gustFactor = (metar.windGustKnots - metar.windSpeedKnots).toDouble()
                addRow(layout, "Add ${(gustFactor / 2).roundToInt()} kt to your normal approach speed")
            }
            if (metar.wshft) {
                val sb = StringBuilder()
                sb.append("Wind shift of 45\u00B0 or more detected during past hour")
                if (metar.fropa) {
                    sb.append(" due to frontal passage")
                }
                addRow(layout, sb.toString())
            }
        }
    }

    private fun addSkyConditionRow(layout: LinearLayout, sky: SkyCondition, flightCategory: String) {
        val row = addRow(layout, sky.toString())
        DetailRowItem2Binding.bind(row).apply {
            showColorizedDrawable(itemLabel, flightCategory, sky.drawable)
        }
    }

    private fun addWeatherRow(layout: LinearLayout, wx: WxSymbol, flightCategory: String) {
        val row = addRow(layout, wx.toString())
        if (wx.drawable != 0) {
            DetailRowItem2Binding.bind(row).apply {
                showColorizedDrawable(itemLabel, flightCategory, wx.drawable)
            }
        }
    }
}