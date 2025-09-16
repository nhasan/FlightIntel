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

import android.os.Bundle
import com.nadmm.airports.R
import com.nadmm.airports.TabPagerActivityBase

class WxMainActivity : TabPagerActivityBase() {

    override val initialTabIndex: Int
        get() = getIndex()

    private val tabEntries = arrayOf(
        R.string.nearby to NearbyWxFragment::class.java,
        R.string.favorites to FavoriteWxFragment::class.java,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle("Weather", null)

        for (entry in tabEntries) {
            addTab(resources.getString(entry.first), entry.second)
        }
    }

    private fun getIndex() : Int {
        if (prefAlwaysShowNearby) {
            return tabEntries.indexOfFirst { it.first == R.string.nearby }
        }

        val fav = dbManager.wxFavorites
        return if (fav.isNotEmpty())
            tabEntries.indexOfFirst { it.first == R.string.favorites }
        else
            tabEntries.indexOfFirst { it.first == R.string.nearby }
    }

    override val selfNavDrawerItem = R.id.navdrawer_wx
}
