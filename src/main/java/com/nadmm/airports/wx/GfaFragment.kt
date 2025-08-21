/*
 * FlightIntel for Pilots
 *
 * Copyright 2017-2025 Nadeem Hasan <nhasan@nadmm.com>
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

class GfaFragment : WxGraphicFragmentBase(NoaaService.ACTION_GET_GFA, regions, forecasts) {
    init {
        title = "Graphical Forecast"
        graphicTypeLabel = "Select Forecast"
        graphicLabel = "Select Region"
    }

    override val serviceIntent: Intent
        get() = Intent(requireActivity(), GfaService::class.java)

    override val product: String = "gfa"

    companion object {
        private val forecasts: Map<String, String> = mapOf(
            "F03_gfa_clouds" to "3 Hour Clouds",
            "F03_gfa_sfc" to "3 Hour Surface",
            "F06_gfa_clouds" to "6 Hour Clouds",
            "F06_gfa_sfc" to "6 Hour Surface",
            "F09_gfa_clouds" to "9 Hour Clouds",
            "F09_gfa_sfc" to "9 Hour Surface",
            "F12_gfa_clouds" to "12 Hour Clouds",
            "F12_gfa_sfc" to  "12 Hour Surface",
            "F15_gfa_clouds" to "15 Hour Clouds",
            "F15_gfa_sfc" to "15 Hour Surface",
            "F18_gfa_clouds" to "18 Hour Clouds",
            "F18_gfa_sfc" to "18 Hour Surface"
        )

        private val regions: Map<String, String> = mapOf(
            "us" to "Continental US",
            "ne" to "Northeast",
            "e" to "East",
            "se" to "Southeast",
            "nc" to "North Central",
            "c" to "Central",
            "sc" to "South Central",
            "nw" to "Northwest",
            "w" to "West",
            "sw" to "Southwest"
        )
    }
}
