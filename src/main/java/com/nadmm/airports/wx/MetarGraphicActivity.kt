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
import android.os.Bundle
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.wx.WxRegions.RegionCodes

class MetarGraphicActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_activity_layout_no_toolbar)

        val args = intent.extras
        addFragment(MetarGraphicFragment::class.java, args)
    }

    override fun onFragmentStarted(fragment: FragmentBase) {
        // Do not call the parent implementation
    }

    class MetarGraphicFragment : WxGraphicFragmentBase(NoaaService.ACTION_GET_METAR, RegionCodes) {
        init {
            graphicLabel = "Select Region"
        }

        override val serviceIntent: Intent
            get() = Intent(activity, MetarService::class.java)
    }
}
