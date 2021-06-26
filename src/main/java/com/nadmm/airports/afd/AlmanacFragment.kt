/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.utils.SolarCalculator
import com.nadmm.airports.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class AlmanacFragment : FragmentBase() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.almanac_detail_view, container, false)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setActionBarTitle("Sunrise and sunset", "")

        arguments?.getString(Airports.SITE_NUMBER)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                showDetails(result)
            }
        }
    }

    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        showAirportTitle(apt)
        showSolarInfo(apt)
        setFragmentContentShown(true)
    }

    @SuppressLint("SetTextI18n")
    private fun showSolarInfo(apt: Cursor) {
        val timezoneId = apt.getString(apt.getColumnIndex(Airports.TIMEZONE_ID))
        val tz = TimeZone.getTimeZone(timezoneId)
        val lat = apt.getDouble(apt.getColumnIndex(Airports.REF_LATTITUDE_DEGREES))
        val lon = apt.getDouble(apt.getColumnIndex(Airports.REF_LONGITUDE_DEGREES))
        val location = Location("")
        location.latitude = lat
        location.longitude = lon
        val now = Calendar.getInstance(tz)

        val solarCalc = SolarCalculator(location, now.timeZone)
        val sunrise = solarCalc.getSunriseTime(SolarCalculator.OFFICIAL, now)
        val sunset = solarCalc.getSunsetTime(SolarCalculator.OFFICIAL, now)
        val morningTwilight = solarCalc.getSunriseTime(SolarCalculator.CIVIL, now)
        val eveningTwilight = solarCalc.getSunsetTime(SolarCalculator.CIVIL, now)

        val format = SimpleDateFormat("HH:mm", Locale.US)
        val local = now.timeZone
        val utc = TimeZone.getTimeZone("GMT")

        findViewById<TextView>(R.id.sunrise_sunset_label)?.let {
            val date = DateFormat.getDateInstance()
            it.text = "Sunrise and Sunset (${date.format(now.time)})"
        }

        findViewById<LinearLayout>(R.id.morning_info_layout)?.apply {
            format.timeZone = local
            addRow(this, "Morning civil twilight (Local)", format.format(morningTwilight.time))
            format.timeZone = utc
            addRow(this, "Morning civil twilight (UTC)", format.format(morningTwilight.time))
        }

        findViewById<LinearLayout>(R.id.sunrise_info_layout)?.let {
            if (sunrise != null) {
                format.timeZone = local
                addRow(it, "Sunrise (Local)", format.format(sunrise.time))
                format.timeZone = utc
                addRow(it, "Sunrise (UTC)", format.format(sunrise.time))
            } else {
                addRow(it, "Sunrise (Local)", "Sun does not rise")
                addRow(it, "Sunrise (UTC)", "Sun does not rise")
            }
        }

        findViewById<LinearLayout>(R.id.sunset_info_layout)?.let {
            if (sunset != null) {
                format.timeZone = local
                addRow(it, "Sunset (Local)", format.format(sunset.time))
                format.timeZone = utc
                addRow(it, "Sunset (UTC)", format.format(sunset.time))
            } else {
                addRow(it, "Sunset (Local)", "Sun does not set")
                addRow(it, "Sunset (UTC)", "Sun does not set")
            }
        }

        findViewById<LinearLayout>(R.id.evening_info_layout)?.let {
            format.timeZone = local
            addRow(it, "Evening civil twilight (Local)", format.format(eveningTwilight.time))
            format.timeZone = utc
            addRow(it, "Evening civil twilight (UTC)", format.format(eveningTwilight.time))
        }

        findViewById<LinearLayout>(R.id.current_time_layout)?.let {
            format.timeZone = local
            addRow(it, "Local time zone", TimeUtils.getTimeZoneAsString(local))
            addRow(it, "Current time (Local)", format.format(now.time))
            format.timeZone = utc
            addRow(it, "Current time (UTC)", format.format(now.time))
            // Determine FAR 1.1 definition of day/night for logging flight time
            var day = now in morningTwilight..eveningTwilight
            addRow(it, "FAR 1.1 day/night", if (day) "Day" else "Night")
            // Determine FAR 61.75(b) definition of day/night for carrying passengers
            if (sunset != null && sunrise != null) {
                val far6175bBegin = sunset.clone() as Calendar
                far6175bBegin.add(Calendar.HOUR_OF_DAY, 1)
                val far6175bEnd = sunrise.clone() as Calendar
                far6175bEnd.add(Calendar.HOUR_OF_DAY, -1)
                day = now in far6175bEnd..far6175bBegin
                addRow(it, "FAR 61.75(b) day/night", if (day) "Day" else "Night")
            }
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val c = getAirportDetails(siteNumber)
        return arrayOf(c)
    }

}
