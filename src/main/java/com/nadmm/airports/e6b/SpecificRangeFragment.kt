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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.R

class SpecificRangeFragment : E6bFragmentBase() {
    private var mFuelTotalEdit: TextInputLayout? = null
    private var mFuelRateEdit: TextInputLayout? = null
    private var mGsEdit: TextInputLayout? = null
    private var mRangeEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_specific_range_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFuelTotalEdit = findViewById(R.id.e6b_edit_total_fuel)
        mFuelRateEdit = findViewById(R.id.e6b_edit_burn_rate)
        mGsEdit = findViewById(R.id.e6b_edit_gs)
        mRangeEdit = findViewById(R.id.e6b_edit_range)
        addEditField(mFuelTotalEdit)
        addEditField(mFuelRateEdit)
        addEditField(mGsEdit)
        addReadOnlyField(mRangeEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "You can directly substitute Gallons with Pounds for Jet fuel"

    override fun processInput() {
        try {
            val fuelTotal = parseDouble(mFuelTotalEdit)
            val fuelRate = parseDouble(mFuelRateEdit)
            val gs = parseDouble(mGsEdit)
            val range = fuelTotal / fuelRate * gs
            showValue(mRangeEdit, range)
        } catch (ignored: NumberFormatException) {
            clearEditText(mRangeEdit)
        }
    }
}