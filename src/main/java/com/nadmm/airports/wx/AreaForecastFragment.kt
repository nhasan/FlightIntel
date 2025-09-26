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

class AreaForecastFragment()
    : WxTextFragmentBase(NoaaService.ACTION_GET_FA, areas) {

    override val title: String = "Area Forecast"
    override val helpText: String = "Refer to GFA (Graphical Forecast for Aviation) tab for other regions."

    override val serviceIntent: Intent
        get() = Intent(requireActivity(), AreaForecastService::class.java)

    companion object {
        private val areas: Map<String, String> = mapOf(
            "aknorth" to "Northern half of Alaska",
            "akcentral" to "Interior Alaska",
            "aksouth" to "Southcentral Alaska",
            "aksouthwest" to "Alaska Penninsula",
            "aksoutheast" to "Eastern Gulf of Alaska",
            "akpanhandle" to "Alaska Panhandle"
        )
    }
}
