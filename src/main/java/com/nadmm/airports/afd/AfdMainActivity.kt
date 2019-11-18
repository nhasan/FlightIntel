/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd

import android.os.Bundle
import com.nadmm.airports.R
import com.nadmm.airports.TabPagerActivityBase

class AfdMainActivity : TabPagerActivityBase() {

    private val mTabTitles = arrayOf(
            R.string.favorites,
            R.string.nearby,
            R.string.browse
    )

    private val mClasses = arrayOf<Class<*>>(
            FavoriteAirportsFragment::class.java,
            NearbyAirportsFragment::class.java,
            BrowseAirportsFragment::class.java
    )

    private val FAVORITES = mTabTitles.indexOf(R.string.favorites)
    private val NEARBY = mTabTitles.indexOf(R.string.nearby)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarTitle("Airports", null)

        val args = Bundle()
        for (i in mTabTitles.indices) {
            addTab(resources.getString(mTabTitles[i]), mClasses[i], args)
        }
    }

    override fun getInitialTabIndex(): Int {
        if (prefAlwaysShowNearby) {
            return NEARBY
        }

        val fav = dbManager.aptFavorites
        return if (fav.isNotEmpty()) FAVORITES else NEARBY
    }

    override fun getSelfNavDrawerItem(): Int {
        return R.id.navdrawer_afd
    }

}
