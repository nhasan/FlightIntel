/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.tfr

import android.os.Bundle
import androidx.core.os.BundleCompat
import com.nadmm.airports.TabPagerActivityBase
import com.nadmm.airports.tfr.TfrList.Tfr

/** A TFR's details and, when it has areas to draw, its map, as swipeable tabs. */
class TfrDetailActivity : TabPagerActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = intent.extras ?: return finish()
        val tfr = BundleCompat.getSerializable(args, TfrListActivity.EXTRA_TFR, Tfr::class.java)
            ?: return finish()

        setActionBarTitle("TFR Details", tfr.name)
        addTab("Text", TfrDetailFragment::class.java, args)
        if (tfr.shapes.isNotEmpty()) {
            addTab("Map", TfrMapFragment::class.java, args)
        }
    }
}
