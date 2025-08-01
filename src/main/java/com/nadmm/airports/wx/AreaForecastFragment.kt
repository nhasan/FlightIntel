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
    : WxTextFragmentBase(NoaaService.ACTION_GET_FA, mAreas) {

    override val title: String
        get() = "Area Forecast"
    override val serviceIntent: Intent
        get() = Intent(getActivity(), AreaForecastService::class.java)
    override val helpText: String
        get() = "Refer to GFA (Graphical Forecast for Aviation) tab for other regions."
    override val product: String?
        get() = "fa"

    companion object {
        private val mAreas = mapOf(
            "fa_alaska_n1.txt" to "Alaska North Part 1",
            "fa_alaska_n2.txt" to "Alaska North Part 2",
            "fa_alaska_sc1.txt" to "Alaska Southcentral Part 1",
            "fa_alaska_sc2.txt" to "Alaska Southcentral Part 2",
            "fa_alaska_se1.txt" to "Alaska Southeast Part 1",
            "fa_alaska_se2.txt" to "Alaska Southeast Part 2",
            "fa_alaska_bswa.txt" to "Alaska Southwest",
            "fa_carib.txt" to "Carribean",
            "fa_gulf.txt" to "Gulf of Mexico",
            "fa_hawaii.txt" to "Hawaii",
        )
    }
}
