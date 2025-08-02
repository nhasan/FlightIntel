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
import com.nadmm.airports.wx.WxRegions.RegionCodes

class CvaFragment : WxGraphicFragmentBase(NoaaService.ACTION_GET_CVA, RegionCodes, TypeCode) {
    init {
        setTitle("Ceiling and Visibility")
        setLabel("Select Region")
        setHelpText(
            ("By FAA policy, CVA is a Supplementary Weather Product for "
                    + "enhanced situational awareness only. CVA must only be used with primary "
                    + "products such as METARs, TAFs and AIRMETs.")
        )
    }

    override val product: String
        get() = "cva"

    override val serviceIntent: Intent
        get() = Intent(activity, CvaService::class.java)

    companion object {
        private val TypeCode = mapOf(
            "fltcat" to "CVA - Flight Category",
            "ceil" to "CVA - Ceiling",
            "vis" to "CVA - Visibility"
        )
    }
}
