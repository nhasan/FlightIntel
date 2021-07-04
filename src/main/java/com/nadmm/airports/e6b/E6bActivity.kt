/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.e6b

import android.os.Bundle
import com.nadmm.airports.FragmentActivityBase
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R

class E6bActivity : FragmentActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = Bundle()
        args.putString(FRAGMENT_TAG_EXTRA, R.id.CATEGORY_MAIN.toString())
        args.putString(ListMenuFragment.SUBTITLE_TEXT, "No More Slide Rule")
        args.putLong(ListMenuFragment.MENU_ID, R.id.CATEGORY_MAIN.toLong())
        addFragment(E6bMenuFragment::class.java, args)
    }

    override val selfNavDrawerItem: Int
        get() = R.id.navdrawer_e6b
}