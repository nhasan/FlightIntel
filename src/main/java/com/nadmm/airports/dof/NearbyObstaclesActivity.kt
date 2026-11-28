/*
 * FlightIntel for Pilots
 *
 * Copyright 2018-2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.dof

import android.os.Bundle
import com.nadmm.airports.FragmentActivityBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.LocationColumns

/**
 * Obstacles near the device's location, or a location passed in, on a map and
 * in a list. [NearbyObstaclesFragment] holds both, as tabs.
 */
class NearbyObstaclesActivity : FragmentActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The fragment gives the subtitle, as it knows the radius
        setActionBarTitle("Nearby Obstacles")
        addFragment(NearbyObstaclesFragment::class.java, intent.extras)
    }

    // Opened for a location, such as from an airport's details, it is a
    // detail screen with a back arrow rather than the drawer's Obstacles screen
    override val selfNavDrawerItem: Int
        get() = if (intent.hasExtra(LocationColumns.LOCATION)) NAVDRAWER_ITEM_INVALID
            else R.id.navdrawer_dof

    companion object {
        /** A name for the location passed in, such as an airport's, shown on the map. */
        const val EXTRA_PLACE_NAME = "PLACE_NAME"
    }
}
