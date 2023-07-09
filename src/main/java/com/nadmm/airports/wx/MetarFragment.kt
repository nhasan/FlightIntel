/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2023 Nadeem Hasan <nhasan@nadmm.com>
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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Awos2
import com.nadmm.airports.data.DatabaseManager.Wxs
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
    private var remarks: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBroadcastFilter(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.metar_detail_view, container, false)
        val btnGraphic = view.findViewById<Button>(R.id.btnViewGraphic)
        btnGraphic.setOnClickListener { _: View? ->
            val intent = Intent(activity, MetarMapActivity::class.java)
            startActivity(intent)
        }
        return createContentView(view)
    }

    override fun onResume() {
        super.onResume()
        arguments?.let {
            val stationId = it.getString(NoaaService.STATION_ID) ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(stationId)
                }
                showDetails(result)
            }
        }
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
        requestMetar(true)
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

    private fun showDetails(result: Array<Cursor?>) {
        val wxs = result[0] ?: return
        if (wxs.moveToFirst()) {
            location = Location("").apply {
                latitude = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_LATITUDE_DEGREES))
                longitude = wxs.getDouble(wxs.getColumnIndexOrThrow(Wxs.STATION_LONGITUDE_DEGREES))
            }
            val rmk = result[2]
            if (rmk?.moveToFirst() == true) {
                remarks.clear()
                do {
                    remarks.add(rmk.getString(rmk.getColumnIndexOrThrow(Awos2.WX_STATION_REMARKS)))
                } while (rmk.moveToNext())
            }
            showWxTitle(result)
            requestMetar(false)
        } else {
            if (activity != null) {
                showToast(requireActivity().applicationContext,
                    "Unable to get weather station info")
                requireActivity().finish()
            }
        }
    }

    private fun requestMetar(refresh: Boolean) {
        val args = arguments
        if (activity != null && args != null) {
            val stationId = args.getString(NoaaService.STATION_ID)
            val service = Intent(activity, MetarService::class.java)
            service.action = action
            val stationIds = ArrayList<String?>()
            stationIds.add(stationId)
            service.putExtra(NoaaService.STATION_IDS, stationIds)
            service.putExtra(NoaaService.TYPE, NoaaService.TYPE_TEXT)
            service.putExtra(NoaaService.HOURS_BEFORE, NoaaService.METAR_HOURS_BEFORE)
            service.putExtra(NoaaService.FORCE_REFRESH, refresh)
            requireActivity().startService(service)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMetar(intent: Intent) {
        val metar = intent.getSerializableExtra(NoaaService.RESULT) as Metar? ?: return
        findViewById<LinearLayout>(R.id.wx_status_layout)?.let { layout ->
            layout.removeAllViews()
            if (!metar.isValid) {
                layout.visibility = View.VISIBLE
                findViewById<TextView>(R.id.status_msg)?.let { tv ->
                    tv.visibility = View.VISIBLE
                    tv.text = "Unable to get METAR for this location"
                }
                addRow(layout, "This could be due to the following reasons:")
                addBulletedRow(layout, "Network connection is not available")
                addBulletedRow(layout, "ADDS does not publish METAR for this station")
                addBulletedRow(layout, "Station is currently out of service")
                addBulletedRow(layout, "Station has not updated the METAR for more than 3 hours")
                findViewById<View>(R.id.wx_detail_layout)?.visibility = View.GONE
                setFragmentContentShown(true)
                return
            } else {
                findViewById<TextView>(R.id.status_msg)?.let { tv ->
                    tv.visibility = View.GONE
                }
                layout.visibility = View.GONE
                findViewById<View>(R.id.wx_detail_layout)?.visibility = View.VISIBLE
            }
        }
        findViewById<TextView>(R.id.wx_station_info)?.let { tv ->
            setFlightCategoryDrawable(tv, metar.flightCategory)
        }
        findViewById<TextView>(R.id.wx_age)?.let { tv ->
            tv.text = TimeUtils.formatElapsedTime(metar.observationTime)
        }
        // Raw Text
        findViewById<TextView>(R.id.wx_raw_metar)?.let { tv ->
            tv.text = metar.rawText
        }

        // Winds
        findViewById<LinearLayout>(R.id.wx_wind_layout)?.let { layout ->
            layout.removeAllViews()
            var visibility = View.GONE
            if (metar.windSpeedKnots < Int.MAX_VALUE) {
                showWindInfo(layout, metar)
                visibility = View.VISIBLE
            }
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_wind_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Visibility
        findViewById<LinearLayout>(R.id.wx_vis_layout)?.let { layout ->
            layout.removeAllViews()
            var visibility = View.GONE
            if (metar.visibilitySM < Float.MAX_VALUE) {
                if (metar.flags.contains(Metar.Flags.Auto) && metar.visibilitySM == 10f) {
                    addRow(layout, "10+ statute miles horizontal")
                } else {
                    addRow(layout, String.format("%s statute miles horizontal",
                            formatNumber(metar.visibilitySM)))
                }
                if (metar.vertVisibilityFeet < Int.MAX_VALUE) {
                    addRow(layout, String.format("%s vertical",
                            formatFeetAgl(metar.vertVisibilityFeet.toFloat())))
                }
                visibility = View.VISIBLE
            }
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_vis_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Weather
        findViewById<LinearLayout>(R.id.wx_weather_layout)?.let { layout ->
            layout.removeAllViews()
            for (wx in metar.wxList) {
                addWeatherRow(layout, wx, metar.flightCategory)
            }
            if (metar.ltg) {
                addRow(layout, "Lightning in the vicinity")
            }
            val visibility = if (layout.childCount > 0) View.VISIBLE else View.GONE
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_weather_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Sky Condition
        findViewById<LinearLayout>(R.id.wx_sky_cond_layout)?.let { layout ->
            layout.removeAllViews()
            var visibility = View.GONE
            if (metar.skyConditions.isNotEmpty()) {
                val ceiling = getCeiling(metar.skyConditions)
                if (!listOf("NSC", "OVX").contains(ceiling.skyCover)) {
                    addRow(layout, "Ceiling is " + formatFeetAgl(ceiling.cloudBaseAGL.toFloat()))
                }
                for (sky in metar.skyConditions) {
                    addSkyConditionRow(layout, sky, metar.flightCategory)
                }
                visibility = View.VISIBLE
            }
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_sky_cond_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Temperature
        findViewById<LinearLayout>(R.id.wx_temp_layout)?.let { layout ->
            layout.removeAllViews()
            var visibility = View.GONE
            if (metar.tempCelsius < Float.MAX_VALUE && metar.dewpointCelsius < Float.MAX_VALUE) {
                addRow(layout, "Temperature", formatTemperature(metar.tempCelsius))
                if (metar.dewpointCelsius < Float.MAX_VALUE) {
                    addRow(layout, "Dew point", formatTemperature(metar.dewpointCelsius))
                    addRow(layout, "Relative humidity",
                        String.format(Locale.US, "%.0f%%", getRelativeHumidity(metar)))
                    val denAlt = getDensityAltitude(metar).toLong()
                    addRow(layout, "Density altitude", formatFeet(denAlt.toFloat()))
                } else {
                    addRow(layout, "Dew point", "n/a")
                }
                if (metar.maxTemp6HrCentigrade < Float.MAX_VALUE) {
                    addRow(layout, "6-hr max", formatTemperature(metar.maxTemp6HrCentigrade))
                }
                if (metar.minTemp6HrCentigrade < Float.MAX_VALUE) {
                    addRow(layout, "6-hr min", formatTemperature(metar.minTemp6HrCentigrade))
                }
                if (metar.maxTemp24HrCentigrade < Float.MAX_VALUE) {
                    addRow(layout, "24-hr max", formatTemperature(metar.maxTemp24HrCentigrade))
                }
                if (metar.minTemp24HrCentigrade < Float.MAX_VALUE) {
                    addRow(layout, "24-hr min", formatTemperature(metar.minTemp24HrCentigrade))
                }
                visibility = View.VISIBLE
            }
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_temp_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Pressure
        findViewById<LinearLayout>(R.id.wx_pressure_layout)?.let { layout ->
            layout.removeAllViews()
            var visibility = View.GONE
            if (metar.altimeterHg < Float.MAX_VALUE) {
                addRow(layout, "Altimeter", formatAltimeter(metar.altimeterHg))
                if (metar.seaLevelPressureMb < Float.MAX_VALUE) {
                    addRow(layout, "Sea level pressure",
                        String.format("%s mb", formatNumber(metar.seaLevelPressureMb)))
                }
                val presAlt = getPressureAltitude(metar).toLong()
                addRow(layout, "Pressure altitude", formatFeet(presAlt.toFloat()))
                if (metar.pressureTend3HrMb < Float.MAX_VALUE) {
                    addRow(layout, "3-hr tendency",
                        String.format(Locale.US, "%+.2f mb", metar.pressureTend3HrMb))
                }
                if (metar.presfr) {
                    addRow(layout, "Pressure is falling rapidly")
                }
                if (metar.presrr) {
                    addRow(layout, "Pressure is rising rapidly")
                }
                visibility = View.VISIBLE
            }
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_pressure_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Precipitation
        findViewById<LinearLayout>(R.id.wx_precip_layout)?.let { layout ->
            layout.removeAllViews()
            if (metar.precipInches < Float.MAX_VALUE) {
                addRow(layout, "1-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precipInches))
            }
            if (metar.precip3HrInches < Float.MAX_VALUE) {
                addRow(layout, "3-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precip3HrInches))
            }
            if (metar.precip6HrInches < Float.MAX_VALUE) {
                addRow(layout, "6-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precip6HrInches))
            }
            if (metar.precip24HrInches < Float.MAX_VALUE) {
                addRow(layout, "24-hr precipitation",
                    String.format(Locale.US, "%.2f\"", metar.precip24HrInches))
            }
            if (metar.snowInches < Float.MAX_VALUE) {
                addRow(layout, "Snow depth", String.format(Locale.US, "%.0f\"", metar.snowInches))
            }
            if (metar.snincr) {
                addRow(layout, "Snow is increasing rapidly")
            }
            val visibility = if (layout.childCount > 0) View.VISIBLE else View.GONE
            layout.visibility = visibility
            findViewById<TextView>(R.id.wx_precip_label)?.let { tv ->
                tv.visibility = visibility
            }
        }

        // Remarks
        findViewById<LinearLayout>(R.id.wx_remarks_layout)?.let { layout ->
            layout.removeAllViews()
            for (flag in metar.flags) {
                addBulletedRow(layout, flag.toString())
            }
            for (remark in remarks) {
                addBulletedRow(layout, remark)
            }
            val visibility = if (layout.childCount > 0) View.VISIBLE else View.GONE
            layout.visibility = visibility
            findViewById<View>(R.id.wx_remarks_label)?.visibility = visibility
        }

        // Fetch time
        findViewById<TextView>(R.id.wx_fetch_time)?.let { tv ->
            tv.text = String.format(Locale.US, "Fetched on %s",
                TimeUtils.formatDateTime(activityBase, metar.fetchTime))
            tv.visibility = View.VISIBLE
        }
        setFragmentContentShown(true)
    }

    private fun getWindsDescription(metar: Metar): String {
        val s = StringBuilder()
        if (metar.windDirDegrees == 0 && metar.windSpeedKnots == 0) {
            s.append("Winds are calm")
        } else if (metar.windDirDegrees == 0) {
            s.append(
                String.format(
                    Locale.US, "Winds variable at %d knots",
                    metar.windSpeedKnots
                )
            )
        } else {
            s.append(
                String.format(
                    Locale.US, "From %s (%s true) at %d knots",
                    GeoUtils.getCardinalDirection(metar.windDirDegrees.toFloat()),
                    formatDegrees(metar.windDirDegrees), metar.windSpeedKnots
                )
            )
            if (metar.windGustKnots < Int.MAX_VALUE) {
                s.append(String.format(Locale.US, " gusting to %d knots", metar.windGustKnots))
            }
            if (metar.windPeakKnots < Int.MAX_VALUE
                && metar.windPeakKnots != metar.windGustKnots
            ) {
                s.append(String.format(Locale.US, ", peak at %d knots", metar.windPeakKnots))
            }
        }
        return s.toString()
    }

    private fun showWindInfo(layout: LinearLayout?, metar: Metar) {
        val row = addRow(layout!!, getWindsDescription(metar))
        val tv = row.findViewById<TextView>(R.id.item_label)
        if (metar.windDirDegrees > 0) {
            val declination = GeoUtils.getMagneticDeclination(location)
            val wind = getWindBarbDrawable(tv.context, metar, declination)
            setTextViewDrawable(tv, wind)
        }
        if (metar.windGustKnots < Int.MAX_VALUE) {
            val gustFactor = (metar.windGustKnots - metar.windSpeedKnots).toDouble()
            addRow(layout, String.format(Locale.US, "Add %d knots to your normal approach speed",
                    (gustFactor / 2).roundToInt()))
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

    private fun addSkyConditionRow(layout: LinearLayout, sky: SkyCondition, flightCategory: String) {
        val row = addRow(layout, sky.toString())
        val tv = row.findViewById<TextView>(R.id.item_label)
        showColorizedDrawable(tv, flightCategory, sky.drawable)
    }

    private fun addWeatherRow(layout: LinearLayout, wx: WxSymbol, flightCategory: String) {
        val row = addRow(layout, wx.toString())
        if (wx.drawable != 0) {
            row.findViewById<TextView>(R.id.item_label)?.let { tv ->
                showColorizedDrawable(tv, flightCategory, wx.drawable)
            }
        }
    }
}