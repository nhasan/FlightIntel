/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2022 Nadeem Hasan <nhasan@nadmm.com>
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
import com.nadmm.airports.FragmentActivityBase
import com.nadmm.airports.tfr.TfrList.Tfr

class TfrImageActivity : FragmentActivityBase() {
    private var mTfr: Tfr? = null
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val args = intent.extras
        mTfr = args!!.getSerializable(TfrListActivity.EXTRA_TFR) as Tfr?
        setActionBarTitle(mTfr!!.name!!)
        setActionBarSubtitle("TFR Graphic")
        addFragment(TfrImageFragment::class.java, args)
    }
}