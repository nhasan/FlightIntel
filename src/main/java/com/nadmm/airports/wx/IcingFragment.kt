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

class IcingFragment : WxGraphicFragmentBase(NoaaService.ACTION_GET_ICING, altitudes, types) {
    init {
        graphicTypeLabel = "Select Period"
        title = "Icing Forecast"
        helpText = "Select the forecast period and altitude for icing conditions."
        graphicLabel = "Select Altitude"
    }

    override val serviceIntent: Intent
        get() = Intent(requireActivity(), IcingService::class.java)

    companion object {
        private val types = mapOf(
            "F00_cip" to "Current",
            "F01_fip" to "1 Hour",
            "F02_fip" to "2 hour",
            "F03_fip" to "3 Hour",
            "F06_fip" to "6 Hour",
            "F09_fip" to "9 Hour",
            "F12_fip" to "12 Hour",
            "F15_fip" to "15 Hour",
            "F18_fip" to "18 Hour"
        )

        private val altitudes = mapOf(
            "010" to "1,000 feet MSL",
            "030" to "3,000 feet MSL",
            "060" to "6,000 feet MSL",
            "090" to "9,000 feet MSL",
            "120" to "12,000 feet MSL",
            "150" to "15,000 feet MSL",
            "180" to "18,000 feet MSL",
            "210" to "FL210",
            "240" to "FL240",
            "270" to "FL270",
            "max" to "Max in Column"
        )
    }
}
