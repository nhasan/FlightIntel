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

import android.content.Intent

class ProgChartFragment : WxGraphicFragmentBase(NoaaService.ACTION_GET_PROGCHART, progChartCodes) {
    init {
        title = "Prognosis Charts"
        graphicLabel = "Select Prognosis Chart"
        helpText = "Prognosis charts provide a visual representation of expected weather patterns, " +
                "including temperature, precipitation, and other meteorological elements."
    }

    override val serviceIntent = Intent(requireActivity(), ProgChartService::class.java)

    companion object {
        private val progChartCodes = mapOf(
            "F000_wpc_sfc" to "Current Surface Analysis",
            "F006_wpc_prog" to "6 hr Surface Prognosis",
            "F012_wpc_prog" to "12 hr Surface Prognosis",
            "F018_wpc_prog" to "18 hr Surface Prognosis",
            "F024_wpc_prog" to "24 hr Surface Prognosis",
            "F030_wpc_prog" to "30 hr Surface Prognosis",
            "F036_wpc_prog" to "36 hr Surface Prognosis",
            "F048_wpc_prog" to "48 hr Surface Prognosis",
            "F060_wpc_prog" to "60 hr Surface Prognosis",
            "F072_wpc_prog" to "3 day Surface Prognosis",
            "F096_wpc_prog" to "4 day Surface Prognosis",
            "F120_wpc_prog" to "5 day Surface Prognosis",
            "F144_wpc_prog" to "6 day Surface Prognosis",
            "F168_wpc_prog" to "7 day Surface Prognosis"
        )
    }
}
