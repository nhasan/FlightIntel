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
package com.nadmm.airports.wx

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.location.Location
import android.view.View
import android.widget.TextView
import androidx.cursoradapter.widget.ResourceCursorAdapter
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.WxUtils.getCeiling
import com.nadmm.airports.utils.WxUtils.setColorizedWxDrawable
import java.util.*

@SuppressLint("Range")
class WxCursorAdapter(context: Context?, c: Cursor?, private val mDelegate: WxDelegate) :
    ResourceCursorAdapter(context, R.layout.wx_list_item, c, 0) {
    private class ViewHolder {
        var stationName: TextView? = null
        var stationId: TextView? = null
        var stationInfo: TextView? = null
        var stationInfo2: TextView? = null
        var stationFreq: TextView? = null
        var stationPhone: TextView? = null
        var stationWx: TextView? = null
        var stationWx2: TextView? = null
        var reportAge: TextView? = null
    }

    private fun getViewHolder(view: View): ViewHolder {
        var holder = view.getTag(R.id.TAG_VIEW_HOLDER) as ViewHolder?
        if (holder == null) {
            holder = ViewHolder()
            holder.stationName = view.findViewById(R.id.wx_station_name)
            holder.stationId = view.findViewById(R.id.wx_station_id)
            holder.stationInfo = view.findViewById(R.id.wx_station_info)
            holder.stationInfo2 = view.findViewById(R.id.wx_station_info2)
            holder.stationFreq = view.findViewById(R.id.wx_station_freq)
            holder.stationPhone = view.findViewById(R.id.wx_station_phone)
            holder.stationWx = view.findViewById(R.id.wx_station_wx)
            holder.stationWx2 = view.findViewById(R.id.wx_station_wx2)
            holder.reportAge = view.findViewById(R.id.wx_report_age)
            view.setTag(R.id.TAG_VIEW_HOLDER, holder)
        }
        return holder
    }

    override fun bindView(view: View, context: Context, c: Cursor) {
        val holder = getViewHolder(view)
        val name = c.getString(c.getColumnIndex(Wxs.STATION_NAME))
        if (name != null && name.isNotEmpty()) {
            holder.stationName!!.text = name
        }
        val stationId = c.getString(c.getColumnIndex(Wxs.STATION_ID))
        holder.stationId!!.text = stationId
        view.setTag(R.id.TAG_STATION_ID, stationId)
        val info = StringBuilder()
        val city = c.getString(c.getColumnIndex(Airports.ASSOC_CITY))
        if (city != null && city.isNotEmpty()) {
            info.append(city)
        }
        val state = c.getString(c.getColumnIndex(Airports.ASSOC_STATE))
        if (state != null && state.isNotEmpty()) {
            if (info.isNotEmpty()) {
                info.append(", ")
            }
            info.append(state)
        }
        if (info.isEmpty()) {
            info.append("Location not available")
        }
        holder.stationInfo!!.text = info.toString()
        info.setLength(0)
        var freq = c.getString(c.getColumnIndex(Awos1.STATION_FREQUENCY))
        if (freq == null || freq.isEmpty()) {
            freq = c.getString(c.getColumnIndex(Awos1.SECOND_STATION_FREQUENCY))
        }
        if (freq != null && freq.isNotEmpty()) {
            try {
                info.append(FormatUtils.formatFreq(java.lang.Float.valueOf(freq)))
            } catch (e: NumberFormatException) {
                info.append(freq)
            }
        }
        holder.stationFreq!!.text = info.toString()
        info.setLength(0)
        var type = c.getString(c.getColumnIndex(Awos1.WX_SENSOR_TYPE))
        if (type == null || type.isEmpty()) {
            type = "ASOS/AWOS"
        }
        info.append(type)
        if (c.getColumnIndex(LocationColumns.DISTANCE) >= 0
            && c.getColumnIndex(LocationColumns.BEARING) >= 0
        ) {
            val distance = c.getFloat(c.getColumnIndex(LocationColumns.DISTANCE))
            val bearing = c.getFloat(c.getColumnIndex(LocationColumns.BEARING))
            info.append(", ")
            info.append(FormatUtils.formatNauticalMiles(distance))
            info.append(" ")
            info.append(GeoUtils.getCardinalDirection(bearing))
        } else {
            info.append(", ")
            val elevation = c.getInt(c.getColumnIndex(Wxs.STATION_ELEVATOIN_METER))
            info.append(FormatUtils.formatFeetMsl(DataUtils.metersToFeet(elevation).toFloat()))
        }
        holder.stationInfo2!!.text = info.toString()
        info.setLength(0)
        val phone = c.getString(c.getColumnIndex(Awos1.STATION_PHONE_NUMBER))
        if (phone != null && phone.isNotEmpty()) {
            info.append(phone)
        }
        holder.stationPhone!!.text = info.toString()
        showMetarInfo(view, c, mDelegate.getMetar(stationId))
    }

    @SuppressLint("SetTextI18n")
    fun showMetarInfo(view: View, c: Cursor, metar: Metar?) {
        val holder = getViewHolder(view)
        if (metar != null && metar.isValid) {
            // We have METAR for this station
            try {
                val lat = c.getDouble(
                    c.getColumnIndex(Wxs.STATION_LATITUDE_DEGREES)
                )
                val lon = c.getDouble(
                    c.getColumnIndex(Wxs.STATION_LONGITUDE_DEGREES)
                )
                val location = Location("")
                location.latitude = lat
                location.longitude = lon
                val declination = GeoUtils.getMagneticDeclination(location)
                setColorizedWxDrawable(holder.stationName!!, metar, declination)
            } catch (e: Exception) {
            }
            val info = StringBuilder()
            info.append(metar.flightCategory)
            if (metar.wxList.size > 0) {
                for (wx in metar.wxList) {
                    if (wx.symbol != "NSW") {
                        info.append(", ")
                        info.append(wx.toString().lowercase())
                    }
                }
            }
            if (metar.visibilitySM < Float.MAX_VALUE) {
                info.append(", ")
                info.append(FormatUtils.formatStatuteMiles(metar.visibilitySM))
            }
            if (metar.windSpeedKnots < Int.MAX_VALUE) {
                info.append(", ")
                if (metar.windSpeedKnots == 0) {
                    info.append("calm")
                } else if (metar.windGustKnots < Int.MAX_VALUE) {
                    info.append(
                        String.format(
                            Locale.US, "%dG%dKT",
                            metar.windSpeedKnots, metar.windGustKnots
                        )
                    )
                } else {
                    info.append(String.format(Locale.US, "%dKT", metar.windSpeedKnots))
                }
                if (metar.windSpeedKnots > 0 && metar.windDirDegrees >= 0 && metar.windDirDegrees < Int.MAX_VALUE) {
                    info.append("/")
                    info.append(FormatUtils.formatDegrees(metar.windDirDegrees))
                }
            }
            holder.stationWx?.let {
                it.visibility = View.VISIBLE
                it.text = info.toString()
            }
            info.setLength(0)
            var sky = getCeiling(metar.skyConditions)
            val ceiling = sky.cloudBaseAGL
            var skyCover = sky.skyCover
            if (skyCover == "OVX") {
                info.append("Ceiling indefinite")
            } else if (skyCover != "NSC") {
                info.append("Ceiling ")
                info.append(FormatUtils.formatFeet(ceiling.toFloat()))
            } else {
                if (metar.skyConditions.isNotEmpty()) {
                    sky = metar.skyConditions[0]
                    skyCover = sky.skyCover
                    if (skyCover == "CLR" || skyCover == "SKC") {
                        info.append("Sky clear")
                    } else if (skyCover != "SKM") {
                        info.append(skyCover)
                        info.append(" ")
                        info.append(FormatUtils.formatFeet(sky.cloudBaseAGL.toFloat()))
                    }
                }
            }
            if (info.isNotEmpty()) {
                info.append(", ")
            }

            // Do some basic sanity checks on values
            if (metar.tempCelsius < Float.MAX_VALUE
                && metar.dewpointCelsius < Float.MAX_VALUE
            ) {
                info.append(FormatUtils.formatTemperatureF(metar.tempCelsius))
                info.append("/")
                info.append(FormatUtils.formatTemperatureF(metar.dewpointCelsius))
                info.append(", ")
            }
            if (metar.altimeterHg < Float.MAX_VALUE) {
                info.append(FormatUtils.formatAltimeterHg(metar.altimeterHg))
            }
            holder.stationWx2?.let {
                it.visibility = View.VISIBLE
                it.text = info.toString()
            }
            holder.reportAge?.let {
                it.visibility = View.VISIBLE
                it.text = TimeUtils.formatElapsedTime(metar.observationTime)
            }
        } else {
            setColorizedWxDrawable(holder.stationName!!, metar, 0f)
            if (metar != null) {
                holder.stationWx!!.text = "Wx station in inoperative"
            } else {
                holder.stationWx!!.text = "Wx not fetched"
            }
            holder.stationWx2!!.visibility = View.GONE
            holder.reportAge!!.visibility = View.GONE
        }
    }
}