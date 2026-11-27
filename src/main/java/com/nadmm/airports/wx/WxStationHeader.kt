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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.net.toUri
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.WxStationHeaderBinding
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.UiUtils
import com.nadmm.airports.utils.WxUtils

/**
 * The weather station header: its name, type and place, elevation, the
 * favorite star, and its phone and frequencies.
 */
class WxStationHeader(private val activity: ActivityBase, val binding: WxStationHeaderBinding) {

    init {
        // Report ages belong to each tab's own report
        binding.wxSubtitle.wxAge.visibility = View.GONE
    }

    /**
     * Fills the header from a station cursor ([Wxs]) and its AWOS and
     * airport details ([Awos1] with the airport's city and state).
     */
    @SuppressLint("SetTextI18n")
    fun show(wxs: Cursor, awos: Cursor?) {
        val title = binding.wxTitle
        val subtitle = binding.wxSubtitle
        if (!wxs.moveToFirst()) return
        val icaoCode = wxs.getString(wxs.getColumnIndexOrThrow(Wxs.STATION_ID))
        val stationName = wxs.getString(wxs.getColumnIndexOrThrow(Wxs.STATION_NAME))
        title.wxStationName.text = "$icaoCode - $stationName"
        title.wxStationInfo.text = "ASOS/AWOS"
        if (awos != null && awos.moveToFirst()) {
            val type = awos.getString(awos.getColumnIndexOrThrow(Awos1.WX_SENSOR_TYPE))
                .takeUnless { it.isNullOrBlank() } ?: "ASOS/AWOS"
            val city = awos.getString(awos.getColumnIndexOrThrow(Airports.ASSOC_CITY))
            val state = awos.getString(awos.getColumnIndexOrThrow(Airports.ASSOC_STATE))
            title.wxStationInfo.text = "$type, $city, $state"

            awos.getString(awos.getColumnIndexOrThrow(Awos1.STATION_PHONE_NUMBER))
                ?.takeIf { it.isNotBlank() }?.let {
                    subtitle.wxStationPhone.text = it
                    makeClickToCall(subtitle.wxStationPhone)
                    subtitle.wxStationPhone.visibility = View.VISIBLE
                }
            showFrequency(subtitle.wxStationFreq,
                awos.getString(awos.getColumnIndexOrThrow(Awos1.STATION_FREQUENCY)))
            showFrequency(subtitle.wxStationFreq2,
                awos.getString(awos.getColumnIndexOrThrow(Awos1.SECOND_STATION_FREQUENCY)))
        }

        val meters = wxs.getInt(wxs.getColumnIndexOrThrow(Wxs.STATION_ELEVATOIN_METER))
        val elev = FormatUtils.formatFeet(DataUtils.metersToFeet(meters).toFloat())
        title.wxStationInfo2.text = "Located at $elev MSL elevation"

        val dbManager = activity.dbManager
        title.airportStar.isChecked = dbManager.isFavoriteWx(icaoCode)
        title.airportStar.setOnClickListener { v ->
            if ((v as CheckBox).isChecked) dbManager.addToFavoriteWx(icaoCode)
            else dbManager.removeFromFavoriteWx(icaoCode)
        }
    }

    /** The station's current flight category, from its latest METAR. */
    fun showFlightCategory(flightCategory: String) {
        WxUtils.setFlightCategoryDrawable(binding.wxTitle.wxStationInfo, flightCategory)
    }

    private fun showFrequency(tv: TextView, freq: String?) {
        if (freq.isNullOrBlank()) return
        UiUtils.setTextViewDrawable(tv, R.drawable.ic_outline_antenna_16)
        tv.text = freq
        tv.visibility = View.VISIBLE
    }

    private fun makeClickToCall(tv: TextView) {
        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return
        UiUtils.setTextViewDrawable(tv, R.drawable.ic_outline_phone_16)
        tv.setOnClickListener {
            val phone = DataUtils.decodePhoneNumber(tv.text.toString())
            activity.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
        }
    }
}
