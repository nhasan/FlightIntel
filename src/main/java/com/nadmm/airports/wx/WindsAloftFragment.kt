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

class WindsAloftFragment : WxTextFragmentBase(NoaaService.ACTION_GET_FB, areas, types) {

    override val title: String = "Winds Aloft"
    override val helpText: String = "Winds Aloft forecasts provide wind and temperature information at " +
            "various altitudes, typically used for flight planning. " +
            "The forecast is available for 6, 12, or 24 hours."

    override val serviceIntent: Intent
        get() = Intent(requireActivity(), WindsAloftService::class.java)

    companion object {
        private val types = mapOf<String, String>(
            "06" to "6 Hour",
            "12" to "12 Hour",
            "24" to "24 Hour"
        )

        private val areas = mapOf<String, String>(
            "us" to "Continental US",
            "alaska" to "Alaska",
            "bos" to "Boston",
            "canada" to "Canada",
            "chi" to "Chicago",
            "dfw" to "Dallas/Fort Worth",
            "hawaii" to "Hawaii",
            "mia" to "Miami",
            "pacific" to "Pacific",
            "sfo" to "San Francisco",
            "slc" to "Salt Lake City"
        )
    }
}
