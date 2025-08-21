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

import android.os.Bundle
import com.nadmm.airports.TabPagerActivityBase

class WxDetailActivity : TabPagerActivityBase() {
    private val tabNameFragmentMap: Map<String, Class<*>> = mapOf(
        "METAR" to MetarFragment::class.java,
        "TAF" to TafFragment::class.java,
        "PIREP" to PirepFragment::class.java,
        "GFA" to GfaFragment::class.java,
        "AREA FORECAST" to AreaForecastFragment::class.java,
        "AIRMET/SIGMET" to AirSigmetFragment::class.java,
        "PROG CHARTS" to ProgChartFragment::class.java,
        "WINDS ALOFT" to WindsAloftFragment::class.java,
        "ICING" to IcingFragment::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle("Weather", null)

        val args = intent.extras
        tabNameFragmentMap.forEach { name, fragment ->
            addTab(name, fragment, args)
        }
    }
}
