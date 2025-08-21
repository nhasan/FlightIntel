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

class AirSigmetGraphicFragment : WxGraphicFragmentBase(
    NoaaService.ACTION_GET_AIRSIGMET,
    AirSigmetCodes
) {
    init {
        title = "AIRMET/SIGMET Graphics"
        graphicLabel = "Select Category"
    }

    override val serviceIntent: Intent
        get() = Intent(requireActivity(), AirSigmetService::class.java)

    companion object {
        private val AirSigmetCodes = mapOf(
            "all" to "All active SIGMETs",
            "cb" to "Convective SIGMETs and Outlooks",
            "ic" to "Icing SIGMETs",
            "if" to "IFR SIGMETs",
            "tb" to "Turbulence SIGMETs"
        )
    }
}