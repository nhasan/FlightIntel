/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.TabPagerActivityBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.Awos1
import com.nadmm.airports.data.DatabaseManager.Wxs
import com.nadmm.airports.databinding.WxStationHeaderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A weather station's reports and charts as tabs, under the station's header. */
class WxDetailActivity : TabPagerActivityBase() {
    private var header: WxStationHeader? = null
    private val tabNameFragmentMap = mapOf(
        "METAR" to MetarFragment::class.java,
        "TAF" to TafFragment::class.java,
        "WINDS ALOFT" to WindsAloftFragment::class.java,
        "PIREP" to PirepFragment::class.java,
        "GFA" to GfaFragment::class.java,
        "AREA FORECAST" to AreaForecastFragment::class.java,
        "AIRMET/SIGMET" to AirSigmetGraphicFragment::class.java,
        "PROG CHARTS" to ProgChartFragment::class.java,
        "ICING" to IcingFragment::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle("Weather", null)

        val args = intent.extras
        tabNameFragmentMap.forEach { name, fragment ->
            addTab(name, fragment, args)
        }

        // The station once, above the tabs, rather than on each tab
        val stationId = args?.getString(NoaaService.STATION_ID) ?: return
        val stationHeader = WxStationHeader(this, WxStationHeaderBinding.inflate(layoutInflater))
        header = stationHeader
        lifecycleScope.launch {
            val (wxs, awos) = withContext(Dispatchers.IO) { queryStation(stationId) }
            if (wxs != null) {
                stationHeader.show(wxs, awos)
                setPagerHeader(stationHeader.binding.root)
            }
            wxs?.close()
            awos?.close()
        }
    }

    /** Shows the flight category from the station's latest METAR in the header. */
    fun showFlightCategory(flightCategory: String) {
        header?.showFlightCategory(flightCategory)
    }

    private fun queryStation(stationId: String): Pair<Cursor?, Cursor?> {
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS) ?: return null to null
        val wxs = SQLiteQueryBuilder().apply { tables = Wxs.TABLE_NAME }.query(
            db, arrayOf("*"), "${Wxs.STATION_ID}=?", arrayOf(stationId),
            null, null, null, null)
        val awos = SQLiteQueryBuilder().apply {
            tables = "${Airports.TABLE_NAME} a LEFT JOIN ${Awos1.TABLE_NAME} w" +
                    " ON a.${Airports.FAA_CODE} = w.${Awos1.WX_SENSOR_IDENT}"
        }.query(
            db,
            arrayOf(Awos1.WX_SENSOR_IDENT, Awos1.WX_SENSOR_TYPE, Awos1.STATION_FREQUENCY,
                Awos1.SECOND_STATION_FREQUENCY, Awos1.STATION_PHONE_NUMBER,
                Airports.ASSOC_CITY, Airports.ASSOC_STATE),
            "a.${Airports.ICAO_CODE}=?", arrayOf(stationId), null, null, null, null)
        return wxs to awos
    }
}
